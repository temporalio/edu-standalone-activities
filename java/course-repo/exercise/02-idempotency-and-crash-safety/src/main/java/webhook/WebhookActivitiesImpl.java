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
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Override
    public int deliverWebhook(WebhookDelivery req) {
        int attempt = Activity.getExecutionContext().getInfo().getAttempt();
        log.info("Delivering webhook eventId={} attempt={}", req.getEventId(), attempt);
        try {
            String body = MAPPER.writeValueAsString(req.getPayload());
            HttpRequest httpReq = HttpRequest.newBuilder(URI.create(req.getUrl()))
                    .header("Content-Type", "application/json")
                    // TODO: add a stable Idempotency-Key header derived from req.getEventId().
                    // Must be deterministic across retries (use the logical event id, not a random
                    // UUID). Example:
                    // .header("Idempotency-Key", "webhook:" + req.getEventId())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = HTTP.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            // Simulate a transient failure on attempts 1-2 so Temporal retries and the same
            // delivery is POSTed three times. A stable Idempotency-Key keeps the receiver
            // from processing it more than once.
            if (attempt < 3) {
                throw ApplicationFailure.newFailure(
                        "Simulated transient failure on attempt " + attempt, "TransientError");
            }
            return resp.statusCode();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
