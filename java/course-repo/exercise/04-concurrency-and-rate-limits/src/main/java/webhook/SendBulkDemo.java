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

public class SendBulkDemo {
    public static void main(String[] args) {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 60;

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        ActivityClient client = ActivityClient.newInstance(
                service, ActivityClientOptions.newBuilder().setNamespace("default").build());

        List<CompletableFuture<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String seq = String.format("%03d", i);
            StartActivityOptions options = StartActivityOptions.newBuilder()
                    .setId("demo-" + seq)
                    .setTaskQueue(Shared.TASK_QUEUE)
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .build();
            ActivityHandle<Integer> handle = client.start(
                    WebhookActivities.class, WebhookActivities::deliverWebhook, options,
                    new WebhookDelivery(Shared.WEBHOOK_RECEIVER_URL,
                            Map.of("eventId", "demo_" + seq, "type", "demo_rate_limit"), "demo_" + seq));
            futures.add(handle.getResultAsync());
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        System.out.println("All " + count + " deliveries completed.");
    }
}
