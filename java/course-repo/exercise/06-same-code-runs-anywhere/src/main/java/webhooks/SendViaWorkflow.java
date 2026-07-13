package webhooks;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.HashMap;
import java.util.Map;

/** Runs the same deliverWebhook Activity as a single step inside a Workflow. */
public class SendViaWorkflow {
  public static void main(String[] args) {
    String eventId = args.length > 0 ? args[0] : "evt_002";

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);

    WebhookWorkflow workflow =
        client.newWorkflowStub(
            WebhookWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId("wf-" + eventId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .build());

    Map<String, Object> payload = new HashMap<>();
    payload.put("eventId", eventId);
    payload.put("type", "order.created");
    payload.put("amount", 99.99);
    WebhookDelivery req = new WebhookDelivery(Shared.WEBHOOK_RECEIVER_URL, payload, eventId);

    int status = workflow.deliver(req);
    System.out.println("Workflow completed with Activity returning status " + status);
    System.exit(0);
  }
}
