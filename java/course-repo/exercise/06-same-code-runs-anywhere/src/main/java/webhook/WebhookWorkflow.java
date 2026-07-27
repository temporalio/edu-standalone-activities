package webhook;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface WebhookWorkflow {
    @WorkflowMethod
    int run(WebhookDelivery req);
}
