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
        // TODO: POST req.getPayload() as JSON to req.getUrl() using HTTP + MAPPER.
        // TODO: throw a RuntimeException if the response status is >= 300 (Temporal retries).
        // TODO: return the HTTP status code.
        throw new RuntimeException("TODO: implement deliverWebhook (see the Solution tab)");
    }
}
