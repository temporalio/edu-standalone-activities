package webhook;

import io.temporal.api.enums.v1.ActivityIdConflictPolicy;
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
                Map.of("eventId", eventId, "type", "order.created", "amount", 99.99),
                eventId);

        // One API call submits the durable job. USE_EXISTING means a later submit with the
        // same ID (e.g. a retried client call) dedupes instead of erroring.
        StartActivityOptions options = StartActivityOptions.newBuilder()
                .setId("deliver-" + eventId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)
                .build();

        int status = client.execute(
                WebhookActivities.class, WebhookActivities::deliverWebhook, options, req);
        System.out.println("Standalone activity completed with status " + status);
    }
}
