package webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.Activity;
import io.temporal.failure.ApplicationFailure;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookActivitiesImpl implements WebhookActivities {
    private static final Logger log = LoggerFactory.getLogger(WebhookActivitiesImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public int deliverWebhook(WebhookDelivery request) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.info("Delivering webhook eventId={} attempt={}", request.getEventId(), attempt);
        try {
            String body = objectMapper.writeValueAsString(request.getPayload());
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(request.getUrl()))
                    .header("Content-Type", "application/json")
                    // The event id is stable across retries, so every retry POSTs the same
                    // logical delivery key and the receiver dedupes the side effect.
                    .header("Idempotency-Key", "webhook:" + request.getEventId())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode >= 300) {
                // 408 and 429 are transient, so a retry can succeed. Other 3xx and 4xx codes are
                // permanent, so fail fast instead of retrying a broken request forever.
                if (statusCode < 500 && statusCode != 408 && statusCode != 429) {
                    throw ApplicationFailure.newNonRetryableFailure(
                            "HTTP " + statusCode, "WebhookPermanentFailure");
                }
                throw new RuntimeException("HTTP " + statusCode);
            }
            // Simulate a transient failure on attempts 1-2 so Temporal retries and the same
            // delivery is POSTed three times. A stable Idempotency-Key keeps the receiver
            // from processing it more than once.
            if (attempt < 3) {
                throw ApplicationFailure.newFailure(
                        "Simulated transient failure on attempt " + attempt, "TransientError");
            }
            return statusCode;
        } catch (IOException e) {
            // Network error: throw so Temporal retries.
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // The Worker is shutting down or this Activity was cancelled. Restore the
            // interrupt flag so the SDK still sees it, then let Temporal retry.
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
