package webhooks;

import java.util.List;
import java.util.Map;

/**
 * The input to the deliverWebhookBatch Activity: a URL plus a list of items to
 * deliver one at a time. Temporal serializes this to JSON with the default
 * Jackson converter; a public no-arg constructor plus public fields is all it
 * needs.
 */
public class WebhookDeliveryBatch {
  public String url;
  public List<Map<String, Object>> items;

  public WebhookDeliveryBatch() {}

  public WebhookDeliveryBatch(String url, List<Map<String, Object>> items) {
    this.url = url;
    this.items = items;
  }
}
