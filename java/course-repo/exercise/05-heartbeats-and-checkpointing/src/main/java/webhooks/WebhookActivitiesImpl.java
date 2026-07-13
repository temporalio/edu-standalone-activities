package webhooks;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WebhookActivitiesImpl implements WebhookActivities {
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  // Deliver a batch of webhooks as one long-running Standalone Activity. It
  // heartbeats progress after each item so a retry COULD resume from the last
  // checkpoint, but without the read-back below every retry starts over at 0.
  @Override
  public int deliverWebhookBatch(WebhookDeliveryBatch req) {
    ActivityExecutionContext ctx = Activity.getExecutionContext();

    // TODO: on retry, read heartbeatDetails to resume from the last checkpoint.
    // ctx.getHeartbeatDetails(Integer.class) returns the value the previous
    // attempt last passed to ctx.heartbeat(). Without it, every retry restarts
    // at item 0 and re-delivers items the previous attempt already sent.
    int startIndex = 0;

    int delivered = startIndex;
    for (int i = startIndex; i < req.items.size(); i++) {
      Map<String, Object> item = req.items.get(i);
      post(req.url, item);
      delivered++;
      System.out.println("Delivered item " + i + " (" + delivered + "/" + req.items.size() + ")");
      // Report progress to Temporal. The server stores this so the NEXT attempt
      // can read it back with ctx.getHeartbeatDetails(Integer.class). heartbeat()
      // is also where cancellation is delivered.
      ctx.heartbeat(delivered);
      try {
        Thread.sleep(1000); // Slow enough to run kill-worker.sh mid-batch in the demo.
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    return delivered;
  }

  // POST one item to the receiver as JSON; throw on 4xx/5xx so Temporal retries.
  private void post(String url, Map<String, Object> item) {
    try {
      String body = MAPPER.writeValueAsString(item);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new RuntimeException("HTTP " + response.statusCode());
      }
    } catch (java.io.IOException | InterruptedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
