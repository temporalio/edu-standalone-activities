package webhooks;

import java.util.Map;

/**
 * The input for a single webhook delivery. Kept here as the base shape; the
 * batch Activity in this module works with {@link WebhookDeliveryBatch}.
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
