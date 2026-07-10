package webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
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
    public int deliverWebhookBatch(WebhookDeliveryBatch req) {
        ActivityExecutionContext ctx = Activity.getExecutionContext();

        int startIndex = 0;
        // TODO 1: if a heartbeat checkpoint exists, resume from it instead of starting at 0:
        //   java.util.Optional<Integer> checkpoint = ctx.getHeartbeatDetails(Integer.class);
        //   if (checkpoint.isPresent()) { startIndex = checkpoint.get(); }

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
                // TODO 2: ctx.heartbeat(delivered) to checkpoint each item.
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return delivered;
    }
}
