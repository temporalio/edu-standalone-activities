package webhook;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendStandalone {
    private static final Logger log = LoggerFactory.getLogger(SendStandalone.class);

    public static void main(String[] args) {
        String eventId = args.length > 0 ? args[0] : "evt_001";

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        ActivityClient client = ActivityClient.newInstance(
                service, ActivityClientOptions.newBuilder().setNamespace("default").build());

        WebhookDelivery request = new WebhookDelivery(
                Webhook.RECEIVER_URL,
                Map.of("eventId", eventId, "type", "order.created", "amount", 99.99),
                eventId);

        // One API call submits the durable job. No Workflow, no broker.
        StartActivityOptions options = StartActivityOptions.newBuilder()
                .setId("deliver-" + eventId)
                .setTaskQueue(Webhook.TASK_QUEUE)
                .setStartToCloseTimeout(Duration.ofSeconds(30))
                .build();

        int status = client.execute(
                WebhookActivities.class, WebhookActivities::deliverWebhook, options, request);
        log.info("Standalone Activity completed with status {}", status);
    }
}
