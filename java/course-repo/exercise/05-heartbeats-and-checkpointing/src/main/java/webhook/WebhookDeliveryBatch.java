package webhook;

import java.util.List;
import java.util.Map;

/**
 * Input to deliverWebhookBatch: a list of items to POST one at a time, with per-item
 * progress checkpointed via heartbeat.
 */
public class WebhookDeliveryBatch {
    private String url;
    private List<Map<String, Object>> items;

    public WebhookDeliveryBatch() {}

    public WebhookDeliveryBatch(String url, List<Map<String, Object>> items) {
        this.url = url;
        this.items = items;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
}
