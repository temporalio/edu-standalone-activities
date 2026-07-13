package webhooks;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * The Activity interface. deliverWebhookBatch delivers a list of items as a
 * single long-running Activity, heartbeating progress after each item. The same
 * interface works whether the Activity runs standalone or inside a Workflow.
 */
@ActivityInterface
public interface WebhookActivities {
  @ActivityMethod
  int deliverWebhookBatch(WebhookDeliveryBatch req);
}
