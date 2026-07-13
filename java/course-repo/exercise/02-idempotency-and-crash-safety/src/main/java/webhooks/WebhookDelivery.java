package webhooks;

import java.util.Map;

/**
 * The input to the deliverWebhook Activity. Temporal serializes this to JSON
 * (via the default Jackson data converter) when the job is enqueued, and
 * deserializes it on the Worker. A public no-arg constructor plus public
 * fields is all Jackson needs.
 */
public class WebhookDelivery {
  public String url;
  public Map<String, Object> payload;
  public String eventId;

  public WebhookDelivery() {}

  public WebhookDelivery(String url, Map<String, Object> payload, String eventId) {
    this.url = url;
    this.payload = payload;
    this.eventId = eventId;
  }
}
