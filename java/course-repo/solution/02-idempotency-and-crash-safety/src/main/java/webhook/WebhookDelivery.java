package webhook;

import java.util.Map;

/** Input to deliverWebhook, whether run as a Standalone Activity or inside a Workflow. */
public class WebhookDelivery {
    private String url;
    private Map<String, Object> payload;
    private String eventId;

    public WebhookDelivery() {}

    public WebhookDelivery(String url, Map<String, Object> payload, String eventId) {
        this.url = url;
        this.payload = payload;
        this.eventId = eventId;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
}
