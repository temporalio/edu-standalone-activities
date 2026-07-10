package webhook;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.common.RetryOptions;
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

        // One API call submits the durable job. The job survives even if this process exits
        // before the Activity completes: the Worker keeps retrying and the result stays
        // available server-side until it's fetched.
        StartActivityOptions options = StartActivityOptions.newBuilder()
                .setId("deliver-" + eventId)
                .setTaskQueue(Shared.TASK_QUEUE)
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                // Temporal's default retry policy is unbounded; bound it for a broken receiver.
                .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(5).build())
                .build();

        int status = client.execute(
                WebhookActivities.class, WebhookActivities::deliverWebhook, options, req);
        System.out.println("Activity completed with status " + status);
    }
}
