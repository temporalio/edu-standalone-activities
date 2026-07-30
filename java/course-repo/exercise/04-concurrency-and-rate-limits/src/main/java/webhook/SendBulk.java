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
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendBulk {
    private static final Logger log = LoggerFactory.getLogger(SendBulk.class);

    public static void main(String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 60;

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        ActivityClient client = ActivityClient.newInstance(
                service, ActivityClientOptions.newBuilder().setNamespace("default").build());

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String sequence = String.format("%03d", i);
            StartActivityOptions options = StartActivityOptions.newBuilder()
                    .setId("bulk-" + sequence)
                    .setTaskQueue(Webhook.TASK_QUEUE)
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build();
            ActivityHandle<Integer> handle = client.start(
                    WebhookActivities.class, WebhookActivities::deliverWebhook, options,
                    new WebhookDelivery(Webhook.RECEIVER_URL,
                            Map.of("eventId", "bulk_" + sequence, "type", "bulk_send"), "bulk_" + sequence));
            futures.add(handle.getResultAsync());
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("All {} deliveries completed.", count);
    }
}
