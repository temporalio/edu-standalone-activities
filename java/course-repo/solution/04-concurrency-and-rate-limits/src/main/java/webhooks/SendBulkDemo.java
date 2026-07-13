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
 * Rate-limit pain demo: fan out N deliveries using demo-* activity IDs.
 *
 * Separate from SendBulk so leftover in-flight retries from this demo do not
 * collide with the bulk-* IDs used in sections 1, 3, and 4.
 */
public class SendBulkDemo {
  public static void main(String[] args) {
    int count = args.length > 0 ? Integer.parseInt(args[0]) : 60;

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    ActivityClient client =
        ActivityClient.newInstance(
            service, ActivityClientOptions.newBuilder().setNamespace("default").build());

    List<ActivityHandle<Integer>> handles = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String eventId = String.format("demo_%03d", i);
      Map<String, Object> payload = new HashMap<>();
      payload.put("eventId", eventId);
      payload.put("type", "demo_rate_limit");
      WebhookDelivery req = new WebhookDelivery(Shared.WEBHOOK_RECEIVER_URL, payload, eventId);

      StartActivityOptions options =
          StartActivityOptions.newBuilder()
              .setId(String.format("demo-%03d", i))
              .setTaskQueue(Shared.TASK_QUEUE)
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              .build();

      handles.add(
          client.start(WebhookActivities.class, WebhookActivities::deliverWebhook, options, req));
    }

    // Blocks: with the receiver rate-limited, these Activities keep retrying on
    // every 429 and never all complete. Press Ctrl+C after watching the 429s.
    for (ActivityHandle<Integer> handle : handles) {
      handle.getResult();
    }

    System.out.println("All " + count + " deliveries completed.");
    System.exit(0);
  }
}
