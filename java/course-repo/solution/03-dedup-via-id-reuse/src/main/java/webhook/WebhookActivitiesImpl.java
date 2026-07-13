package webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        log.info("Delivering webhook eventId={} url={}", req.getEventId(), req.getUrl());
        try {
            String body = MAPPER.writeValueAsString(req.getPayload());
            HttpRequest httpReq = HttpRequest.newBuilder(URI.create(req.getUrl()))
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", "webhook:" + req.getEventId())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = HTTP.send(httpReq, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 300) {
                throw new RuntimeException("HTTP " + resp.statusCode());
            }
            // Tiny pause so the second duplicate start lands while this Activity is still
            // in-flight (idConflictPolicy applies; idReusePolicy does not).
            Thread.sleep(1000);
            return resp.statusCode();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
