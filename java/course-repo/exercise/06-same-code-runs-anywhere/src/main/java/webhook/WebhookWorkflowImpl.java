package webhook;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/** Runs the SAME deliverWebhook Activity as a Workflow step. */
public class WebhookWorkflowImpl implements WebhookWorkflow {
    private final WebhookActivities activities = Workflow.newActivityStub(
            WebhookActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .build());

    @Override
    public int run(WebhookDelivery request) {
        return activities.deliverWebhook(request);
    }
}
