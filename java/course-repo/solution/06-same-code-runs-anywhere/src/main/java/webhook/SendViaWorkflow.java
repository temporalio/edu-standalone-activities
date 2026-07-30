package webhook;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendViaWorkflow {
    private static final Logger log = LoggerFactory.getLogger(SendViaWorkflow.class);

    public static void main(String[] args) {
        String eventId = args.length > 0 ? args[0] : "evt_002";

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WebhookDelivery request = new WebhookDelivery(
                Webhook.RECEIVER_URL,
                Map.of("eventId", eventId, "type", "order.created", "amount", 99.99),
                eventId);

        WebhookWorkflow workflow = client.newWorkflowStub(
                WebhookWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("wf-" + eventId)
                        .setTaskQueue(Webhook.TASK_QUEUE)
                        .build());

        int status = workflow.run(request);
        log.info("Workflow completed with Activity returning status {}", status);
    }
}
