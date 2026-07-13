package webhooks;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * The Activity interface. The very same interface is used whether deliverWebhook
 * runs as a Standalone Activity (started from a client) or as a step inside a
 * Workflow. Standalone vs. in-Workflow is decided by how it is called, not how
 * it is defined.
 */
@ActivityInterface
public interface WebhookActivities {
  @ActivityMethod
  int deliverWebhook(WebhookDelivery req);
}
