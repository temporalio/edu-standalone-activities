package webhook;

import io.temporal.activity.ActivityInterface;

/**
 * A regular annotated interface. Standalone vs. inside-a-Workflow is decided by
 * HOW the Activity is called, not how it's defined.
 */
@ActivityInterface
public interface WebhookActivities {
    int deliverWebhookBatch(WebhookDeliveryBatch req);
}
