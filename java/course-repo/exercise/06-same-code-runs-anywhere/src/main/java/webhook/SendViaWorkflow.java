package webhook;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Map;

public class SendViaWorkflow {
    public static void main(String[] args) {
        String eventId = args.length > 0 ? args[0] : "evt_002";

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WebhookDelivery req = new WebhookDelivery(
                Shared.WEBHOOK_RECEIVER_URL,
                Map.of("eventId", eventId, "type", "order.created", "amount", 99.99),
                eventId);

        // Same Activity, submitted as a Workflow step instead of directly.
        WebhookWorkflow workflow = client.newWorkflowStub(
                WebhookWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("wf-" + eventId)
                        .setTaskQueue(Shared.TASK_QUEUE)
                        .build());

        int status = workflow.run(req);
        System.out.println("Workflow completed with Activity returning status " + status);
    }
}
