package webhooks;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityHandle;
import io.temporal.client.StartActivityOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Submit a batch webhook delivery as a single long-running Standalone Activity.
 *
 * The Activity heartbeats progress between items. Take the Worker service down
 * mid-batch and the next attempt resumes from the last reported checkpoint
 * instead of redoing items already delivered.
 */
public class SendBatch {
  public static void main(String[] args) {
    int count = args.length > 0 ? Integer.parseInt(args[0]) : 10;

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    ActivityClient client =
        ActivityClient.newInstance(
            service, ActivityClientOptions.newBuilder().setNamespace("default").build());

    List<Map<String, Object>> items = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Map<String, Object> item = new HashMap<>();
      item.put("eventId", String.format("item_%03d", i));
      item.put("type", "batch.delivery");
      item.put("index", i);
      items.add(item);
    }
    WebhookDeliveryBatch req = new WebhookDeliveryBatch(Shared.WEBHOOK_RECEIVER_URL, items);

    StartActivityOptions options =
        StartActivityOptions.newBuilder()
            .setId("deliver-batch-" + count)
            .setTaskQueue(Shared.TASK_QUEUE)
            // Comfortably larger than the batch body (count items x ~1s each).
            .setStartToCloseTimeout(Duration.ofMinutes(5))
            // If no heartbeat for 5s, Temporal treats the attempt as dead and
            // retries, picking up from the last stored checkpoint.
            .setHeartbeatTimeout(Duration.ofSeconds(5))
            .build();

    // start() durably enqueues the Activity and returns a handle immediately,
    // without tethering this starter to the Activity's execution. That is what
    // lets you take the Worker service down in the same terminal mid-batch.
    ActivityHandle<Integer> handle =
        client.start(WebhookActivities.class, WebhookActivities::deliverWebhookBatch, options, req);

    // Block on the handle for the final result so the demo can print the total.
    int delivered = handle.getResult();

    System.out.println("Batch delivery completed: " + delivered + " items delivered.");
    System.exit(0);
  }
}
