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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebhookActivitiesImpl implements WebhookActivities {
    private static final Logger log = LoggerFactory.getLogger(WebhookActivitiesImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public int deliverWebhookBatch(WebhookDeliveryBatch request) {
        ActivityExecutionContext context = Activity.getExecutionContext();

        int startIndex = 0;
        // TODO 1: on a retry, resume from the last heartbeat checkpoint instead of starting
        //   over at 0, and log that you did so. Section 2 of the assignment has the block.

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
                // TODO 2: report progress to the server here, so a retry can resume from this
                //   item instead of redoing the batch. Section 2 of the assignment has the line.
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
