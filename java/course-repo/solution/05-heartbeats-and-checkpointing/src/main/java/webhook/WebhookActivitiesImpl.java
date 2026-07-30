package webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.failure.ApplicationFailure;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers each item in the batch one at a time, checkpointing progress via heartbeat so a
 * retry can resume instead of redelivering everything from the start.
 */
public class WebhookActivitiesImpl implements WebhookActivities {
    private static final Logger log = LoggerFactory.getLogger(WebhookActivitiesImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public int deliverWebhookBatch(WebhookDeliveryBatch request) {
        ActivityExecutionContext context = Activity.getExecutionContext();

        // On retry, resume from the last checkpoint instead of redoing item 0..n.
        int startIndex = 0;
        Optional<Integer> checkpoint = context.getHeartbeatDetails(Integer.class);
        if (checkpoint.isPresent()) {
            startIndex = checkpoint.get();
            log.info("Resuming from checkpoint startIndex={} attempt={}",
                    startIndex, context.getInfo().getAttempt());
        }

        int delivered = startIndex;
        try {
            for (int i = startIndex; i < request.getItems().size(); i++) {
                String body = objectMapper.writeValueAsString(request.getItems().get(i));
                HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(request.getUrl()))
                        .header("Content-Type", "application/json")
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
                delivered++;
                // Checkpoint after each item; a future retry reads this back.
                context.heartbeat(delivered);
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            // Network error: throw so Temporal retries.
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // The Worker is shutting down or this Activity was cancelled. Restore the
            // interrupt flag so the SDK still sees it, then let Temporal retry.
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return delivered;
    }
}
