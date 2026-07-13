package webhooks;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * A thin Workflow that calls deliverWebhook as a single step. Not used in
 * Module 01's narrative; Module 06 uses this same pattern to show the exact
 * same Activity running inside a Workflow.
 */
@WorkflowInterface
public interface WebhookWorkflow {
  @WorkflowMethod
  int deliver(WebhookDelivery req);
}
