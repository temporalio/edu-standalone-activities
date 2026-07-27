package webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
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
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Override
    public int deliverWebhookBatch(WebhookDeliveryBatch req) {
        ActivityExecutionContext ctx = Activity.getExecutionContext();

        // TODO: on retry, read ctx.getHeartbeatDetails(Integer.class) to find the last
        // reported progress and resume from there. Without this read, every retry starts at
        // item 0 even though the previous attempt heartbeated its progress.
        int startIndex = 0;

        int delivered = startIndex;
        try {
            for (int i = startIndex; i < req.getItems().size(); i++) {
                String body = MAPPER.writeValueAsString(req.getItems().get(i));
                HttpRequest httpReq = HttpRequest.newBuilder(URI.create(req.getUrl()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> resp = HTTP.send(httpReq, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 300) {
                    throw new RuntimeException("HTTP " + resp.statusCode());
                }
                delivered++;
                // Report progress to Temporal so the NEXT attempt can read it back via
                // getHeartbeatDetails. heartbeat() is also where cancellation is delivered.
                ctx.heartbeat(delivered);
                Thread.sleep(1000); // slow enough to run kill-worker.sh mid-batch in the demo
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return delivered;
    }
}
