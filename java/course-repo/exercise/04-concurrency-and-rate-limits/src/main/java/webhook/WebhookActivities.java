package webhook;

import io.temporal.activity.ActivityInterface;

/**
 * A regular annotated interface. Standalone vs. inside-a-Workflow is decided by
 * HOW deliverWebhook is called, not how it's defined.
 */
@ActivityInterface
public interface WebhookActivities {
    int deliverWebhook(WebhookDelivery req);
}
