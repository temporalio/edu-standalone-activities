package webhook;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;

import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.Map;

public class SendStandalone {
    public static void main(String[] args) {
        String eventId = args.length > 0 ? args[0] : "evt_001";

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        ActivityClient client = ActivityClient.newInstance(
                service, ActivityClientOptions.newBuilder().setNamespace("default").build());

        WebhookDelivery req = new WebhookDelivery(
                Shared.WEBHOOK_RECEIVER_URL,
                Map.of("eventId", eventId, "type", "order.created"),
                eventId);

        // One API call submits the durable job. No Workflow, no broker.
        StartActivityOptions options = StartActivityOptions.newBuilder()
                .setId("deliver-" + eventId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();

        int status = client.execute(
                WebhookActivities.class, WebhookActivities::deliverWebhook, options, req);
        System.out.println("Standalone activity completed with status " + status);
    }
}
