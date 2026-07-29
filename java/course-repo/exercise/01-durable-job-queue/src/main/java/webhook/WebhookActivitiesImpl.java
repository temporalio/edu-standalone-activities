package webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        log.info("Delivering webhook eventId={} url={}", request.getEventId(), request.getUrl());
        // TODO 1: POST request.getPayload() as JSON to request.getUrl() with httpClient.send(...).
        // TODO 2: on a status >= 300, throw. Use a plain RuntimeException for 5xx, 408, and 429
        //         so Temporal retries; use ApplicationFailure.newNonRetryableFailure for the rest.
        // TODO 3: return the response status code on success.
        // Delete the placeholder below; your code replaces it.
        throw new RuntimeException("TODO: implement deliverWebhook");
    }
}
