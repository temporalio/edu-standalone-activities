package webhook;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface WebhookActivities {
    int deliverWebhookBatch(WebhookDeliveryBatch request);
}
