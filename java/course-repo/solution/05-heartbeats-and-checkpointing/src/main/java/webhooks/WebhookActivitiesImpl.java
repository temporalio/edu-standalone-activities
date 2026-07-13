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

  // Deliver a batch of webhooks as one long-running Standalone Activity,
  // heartbeating progress after each item so a retry can resume where it left off.
  @Override
  public int deliverWebhookBatch(WebhookDeliveryBatch req) {
    ActivityExecutionContext ctx = Activity.getExecutionContext();

    // On retry, heartbeatDetails holds the last value the previous attempt passed
    // to ctx.heartbeat(). We checkpoint the count of delivered items, so the retry
    // resumes from there instead of redoing everything from item 0.
    int startIndex = 0;
    Optional<Integer> checkpoint = ctx.getHeartbeatDetails(Integer.class);
    if (checkpoint.isPresent()) {
      startIndex = checkpoint.get();
      System.out.println("Resuming from checkpoint " + startIndex);
    }

    int delivered = startIndex;
    for (int i = startIndex; i < req.items.size(); i++) {
      Map<String, Object> item = req.items.get(i);
      post(req.url, item);
      delivered++;
      System.out.println("Delivered item " + i + " (" + delivered + "/" + req.items.size() + ")");
      // Checkpoint after each item. Temporal stores this so a future retry reads
      // it back with ctx.getHeartbeatDetails(Integer.class). heartbeat() is also
      // where cancellation is delivered.
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
