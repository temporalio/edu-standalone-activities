package webhooks;

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
    System.out.println("Delivering webhook for event " + req.eventId + " to " + req.url);
    try {
      String body = MAPPER.writeValueAsString(req.payload);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(req.url))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build();
      HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
      // Throw on 4xx/5xx. Temporal sees the failure and retries per the retry policy.
      if (response.statusCode() >= 400) {
        throw new RuntimeException("HTTP " + response.statusCode());
      }
      return response.statusCode();
    } catch (java.io.IOException | InterruptedException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
