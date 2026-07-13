package webhooks;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class WebhookWorkflowImpl implements WebhookWorkflow {
  private final WebhookActivities activities =
      Workflow.newActivityStub(
          WebhookActivities.class,
          ActivityOptions.newBuilder()
              .setStartToCloseTimeout(Duration.ofSeconds(10))
              .build());

  @Override
  public int deliver(WebhookDelivery req) {
    // The same deliverWebhook a standalone caller submits directly.
    return activities.deliverWebhook(req);
  }
}
