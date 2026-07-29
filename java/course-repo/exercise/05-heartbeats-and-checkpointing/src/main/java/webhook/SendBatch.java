package webhook;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityHandle;
import io.temporal.client.StartActivityOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendBatch {
    private static final Logger log = LoggerFactory.getLogger(SendBatch.class);

    public static void main(String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 10;

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        ActivityClient client = ActivityClient.newInstance(
                service, ActivityClientOptions.newBuilder().setNamespace("default").build());

        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(Map.of("eventId", String.format("item_%03d", i), "type", "batch.delivery", "index", i));
        }

        StartActivityOptions options = StartActivityOptions.newBuilder()
                .setId("deliver-batch-" + count)
                .setTaskQueue(Webhook.TASK_QUEUE)
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setHeartbeatTimeout(Duration.ofSeconds(5))
                .build();

        ActivityHandle<Integer> handle = client.start(
                WebhookActivities.class, WebhookActivities::deliverWebhookBatch, options,
                new WebhookDeliveryBatch(Webhook.RECEIVER_URL, items));
        int delivered = handle.getResult();
        log.info("Batch delivery completed: {} items delivered.", delivered);
    }
}
