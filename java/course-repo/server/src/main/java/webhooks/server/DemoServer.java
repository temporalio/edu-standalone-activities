package webhooks.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Serves the interactive HTML demos (Modules 05 and 06) as static files so they
 * can be embedded in the Instruqt assignment via an iframe. Listens on :9001.
 */
public class DemoServer {
  private static final int PORT = 9001;
  private static final String DEMOS_DIR =
      System.getenv().getOrDefault("DEMOS_DIR", "/opt/workshop/demos");

  private static final Map<String, String> MIME =
      Map.of(
          ".html", "text/html; charset=utf-8",
          ".js", "application/javascript",
          ".css", "text/css",
          ".png", "image/png",
          ".svg", "image/svg+xml");

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
    server.createContext("/", DemoServer::handle);
    server.start();
    System.out.println("Demo server listening on :" + PORT);
  }

  private static void handle(HttpExchange ex) throws IOException {
    String url = ex.getRequestURI().getPath();
    String rel = url.equals("/") ? "heartbeat-demo/index.html" : url.replaceFirst("^/", "");
    Path filePath = Path.of(DEMOS_DIR, rel).normalize();

    if (!filePath.startsWith(Path.of(DEMOS_DIR)) || !Files.isRegularFile(filePath)) {
      byte[] body = "Not found".getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().set("Content-Type", "text/plain");
      ex.sendResponseHeaders(404, body.length);
      try (OutputStream os = ex.getResponseBody()) {
        os.write(body);
      }
      return;
    }

    String name = filePath.getFileName().toString();
    int dot = name.lastIndexOf('.');
    String ext = dot >= 0 ? name.substring(dot) : "";
    byte[] body = Files.readAllBytes(filePath);
    ex.getResponseHeaders().set("Content-Type", MIME.getOrDefault(ext, "text/plain"));
    ex.getResponseHeaders().set("X-Frame-Options", "ALLOWALL");
    ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
    ex.sendResponseHeaders(200, body.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(body);
    }
  }
}
