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
        // TODO 1: POST req.getPayload() as JSON to req.getUrl() with HTTP.send(...).
        // TODO 2: throw a RuntimeException if the response status is >= 300 (Temporal retries).
        // TODO 3: return the response status code on success.
        throw new RuntimeException("TODO: implement deliverWebhook");
    }
}
