package webhooks;

import io.temporal.activity.Activity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WebhookActivitiesImpl implements WebhookActivities {
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  // Same Activity whether this runs standalone or as a step inside a Workflow.
  @Override
  public int deliverWebhook(WebhookDelivery req) {
    // getAttempt() is 1-based and increases on every Temporal retry.
    int attempt = Activity.getExecutionContext().getInfo().getAttempt();
    System.out.println("Delivering webhook for event " + req.eventId + " (attempt " + attempt + ")");
    try {
      String body = MAPPER.writeValueAsString(req.payload);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(req.url))
              .header("Content-Type", "application/json")
              // TODO: add an Idempotency-Key header derived from req.eventId.
              // The key must be deterministic across retries: use the logical
              // webhook event id (stable), not a random UUID (different every
              // attempt). Format: .header("Idempotency-Key", "webhook:" + req.eventId)
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      // Throw on 4xx/5xx. Temporal sees the failure and retries per the retry policy.
      if (response.statusCode() >= 400) {
        throw new RuntimeException("HTTP " + response.statusCode());
      }

      // Simulate a transient failure after the POST already landed.
      // Real-world equivalent: a 500 from the receiver, a network drop after the
      // receiver processed it, or the process crashing right after the POST.
      // Temporal retries; without an idempotency key the receiver records every attempt.
      if (attempt < 3) {
        System.out.println("Simulated transient failure on attempt " + attempt);
        throw new RuntimeException("Simulated transient failure on attempt " + attempt);
      }

      return response.statusCode();
    } catch (java.io.IOException | InterruptedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
