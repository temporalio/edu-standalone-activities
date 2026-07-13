package webhooks.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A tiny stand-in for the third-party service your webhooks are delivered to.
 * It counts requests, can deduplicate by an Idempotency-Key header, and can
 * enforce a per-second rate limit (returning 429 when exceeded). It has no
 * Temporal dependency; it is plain HTTP. Listens on :9000.
 */
public class WebhookReceiver {
  private static final int PORT = 9000;
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Object LOCK = new Object();

  // Mutable state, guarded by LOCK.
  private static int receivedCount = 0;
  private static int processedCount = 0;
  private static int dedupedCount = 0;
  private static int throttledCount = 0;
  private static int rateLimit = 0;
  private static final List<Long> windowTimestamps = new ArrayList<>();
  private static final Set<String> seenKeys = new HashSet<>();
  private static final List<Map<String, Object>> deliveries = new ArrayList<>();

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
    server.createContext("/", WebhookReceiver::handle);
    server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
    server.start();
    System.out.println("Webhook receiver listening on :" + PORT);
  }

  private static void handle(HttpExchange ex) throws IOException {
    URI uri = ex.getRequestURI();
    String path = uri.getPath();
    String method = ex.getRequestMethod();

    if ("POST".equals(method) && "/hooks".equals(path)) {
      handleHook(ex);
    } else if ("POST".equals(method) && "/_reset".equals(path)) {
      synchronized (LOCK) {
        resetState();
      }
      sendJson(ex, 200, Map.of("reset", true));
    } else if ("POST".equals(method) && "/_rate_limit".equals(path)) {
      int limit = parseLimit(uri.getQuery());
      synchronized (LOCK) {
        rateLimit = limit;
        windowTimestamps.clear();
      }
      sendJson(ex, 200, Map.of("rateLimit", limit));
    } else if ("/_received".equals(path)) {
      Object stats;
      synchronized (LOCK) {
        stats = statsJson();
      }
      sendJson(ex, 200, stats);
    } else if ("GET".equals(method) && (path.equals("/") || path.isEmpty())) {
      sendHtml(ex);
    } else {
      byte[] body = "Not found".getBytes(StandardCharsets.UTF_8);
      ex.sendResponseHeaders(404, body.length);
      try (OutputStream os = ex.getResponseBody()) {
        os.write(body);
      }
    }
  }

  private static void handleHook(HttpExchange ex) throws IOException {
    String raw = readBody(ex);
    Object parsedBody;
    try {
      parsedBody = MAPPER.readValue(raw, Object.class);
    } catch (Exception e) {
      parsedBody = Map.of(); // malformed body; accept anyway
    }
    String idempotencyKey = ex.getRequestHeaders().getFirst("Idempotency-Key");

    synchronized (LOCK) {
      receivedCount++;

      if (isRateLimited()) {
        throttledCount++;
        sendJson(ex, 429, Map.of("error", "Too Many Requests"));
        return;
      }

      if (idempotencyKey != null && seenKeys.contains(idempotencyKey)) {
        dedupedCount++;
        sendJson(ex, 200, Map.of("deduped", true, "idempotencyKey", idempotencyKey));
        return;
      }
      if (idempotencyKey != null) {
        seenKeys.add(idempotencyKey);
      }

      processedCount++;
      Map<String, Object> d = new LinkedHashMap<>();
      d.put("received_at", Instant.now().toString());
      d.put("idempotency_key", idempotencyKey);
      d.put("body", parsedBody);
      deliveries.add(d);

      sendJson(ex, 200, Map.of("ok", true, "processed", processedCount));
    }
  }

  // Caller holds LOCK.
  private static boolean isRateLimited() {
    if (rateLimit == 0) {
      return false;
    }
    long now = System.currentTimeMillis();
    long cutoff = now - 1000;
    windowTimestamps.removeIf(t -> t <= cutoff);
    if (windowTimestamps.size() >= rateLimit) {
      return true;
    }
    windowTimestamps.add(now);
    return false;
  }

  // Caller holds LOCK.
  private static void resetState() {
    receivedCount = 0;
    processedCount = 0;
    dedupedCount = 0;
    throttledCount = 0;
    rateLimit = 0;
    windowTimestamps.clear();
    seenKeys.clear();
    deliveries.clear();
  }

  // Caller holds LOCK.
  private static Map<String, Object> statsJson() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("received_count", receivedCount);
    out.put("processed_count", processedCount);
    out.put("deduped_count", dedupedCount);
    out.put("throttled_count", throttledCount);
    out.put("rate_limit", rateLimit);
    out.put("count", processedCount);
    out.put("deliveries", new ArrayList<>(deliveries));
    return out;
  }

  private static int parseLimit(String query) {
    if (query == null) {
      return 0;
    }
    for (String part : query.split("&")) {
      if (part.startsWith("limit=")) {
        try {
          return Integer.parseInt(part.substring("limit=".length()));
        } catch (NumberFormatException e) {
          return 0;
        }
      }
    }
    return 0;
  }

  private static String readBody(HttpExchange ex) throws IOException {
    try (InputStream is = ex.getRequestBody()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static void sendJson(HttpExchange ex, int status, Object payload) throws IOException {
    byte[] body = MAPPER.writeValueAsBytes(payload);
    ex.getResponseHeaders().set("Content-Type", "application/json");
    ex.sendResponseHeaders(status, body.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(body);
    }
  }

  private static void sendHtml(HttpExchange ex) throws IOException {
    byte[] body = HTML_PAGE.getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", "text/html");
    ex.sendResponseHeaders(200, body.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(body);
    }
  }

  private static final String HTML_PAGE =
      "<!DOCTYPE html>\n"
          + "<html><head><meta charset=\"utf-8\">\n"
          + "<meta http-equiv=\"refresh\" content=\"2\">\n"
          + "<title>Webhook Receiver</title>\n"
          + "<style>\n"
          + "body { font-family: monospace; background: #1a1a2e; color: #e2e8f0; padding: 2rem; }\n"
          + "h1 { color: #7aa2ff; }\n"
          + ".stat { display: inline-block; margin: 0.5rem 1rem 0.5rem 0; padding: 0.5rem 1rem;\n"
          + "        background: #252540; border-radius: 4px; }\n"
          + ".stat .label { color: #a0aec0; font-size: 0.85em; }\n"
          + ".stat .value { font-size: 1.8em; font-weight: bold; color: #9ae6b4; }\n"
          + ".stat .value.red { color: #fc8181; }\n"
          + "pre { background: #252540; padding: 1rem; border-radius: 4px; overflow-x: auto; }\n"
          + "</style>\n"
          + "</head>\n"
          + "<body>\n"
          + "<h1>Webhook Receiver</h1>\n"
          + "<p style=\"color:#a0aec0\">Auto-refreshes every 2 seconds.</p>\n"
          + "<div id=\"stats\"></div>\n"
          + "<pre id=\"json\"></pre>\n"
          + "<script>\n"
          + "fetch('/_received').then(r=>r.json()).then(d=>{\n"
          + "  document.getElementById('json').textContent = JSON.stringify(d, null, 2);\n"
          + "  const stats = document.getElementById('stats');\n"
          + "  const items = [\n"
          + "    ['Received', d.received_count, false],\n"
          + "    ['Processed', d.processed_count, false],\n"
          + "    ['Deduped', d.deduped_count, false],\n"
          + "    ['Throttled (429)', d.throttled_count, d.throttled_count > 0],\n"
          + "    ['Rate limit (req/s)', d.rate_limit || 'off', false],\n"
          + "  ];\n"
          + "  stats.innerHTML = items.map(([l,v,r]) =>\n"
          + "    '<div class=\"stat\"><div class=\"label\">'+l+'</div><div class=\"value'+(r?' red':'')+'\">'\n"
          + "    +v+'</div></div>').join('');\n"
          + "});\n"
          + "</script>\n"
          + "</body></html>";
}
