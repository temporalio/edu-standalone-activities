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
 * Fan out N deliverWebhook Standalone Activities concurrently, then wait for
 * all of them to finish. Used to show how a large batch behaves with and
 * without a Worker rate cap. Uses bulk-* activity IDs.
 */
public class SendBulk {
  public static void main(String[] args) {
    int count = args.length > 0 ? Integer.parseInt(args[0]) : 60;

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    ActivityClient client =
        ActivityClient.newInstance(
            service, ActivityClientOptions.newBuilder().setNamespace("default").build());

    // start() enqueues each Activity and returns immediately with a handle, so
    // the whole batch is submitted concurrently rather than one at a time.
    List<ActivityHandle<Integer>> handles = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String eventId = String.format("bulk_%03d", i);
      Map<String, Object> payload = new HashMap<>();
      payload.put("eventId", eventId);
      payload.put("type", "bulk_send");
      WebhookDelivery req = new WebhookDelivery(Shared.WEBHOOK_RECEIVER_URL, payload, eventId);

      StartActivityOptions options =
          StartActivityOptions.newBuilder()
              .setId(String.format("bulk-%03d", i))
              .setTaskQueue(Shared.TASK_QUEUE)
              .setStartToCloseTimeout(Duration.ofSeconds(30))
              .build();

      handles.add(
          client.start(WebhookActivities.class, WebhookActivities::deliverWebhook, options, req));
    }

    // Block until every delivery finishes.
    for (ActivityHandle<Integer> handle : handles) {
      handle.getResult();
    }

    System.out.println("All " + count + " deliveries completed.");
    System.exit(0);
  }
}
