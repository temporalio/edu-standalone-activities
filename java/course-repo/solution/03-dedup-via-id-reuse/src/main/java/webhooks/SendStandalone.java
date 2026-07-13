package webhooks;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Submits deliverWebhook as a Standalone Activity and waits for the result. */
public class SendStandalone {
  public static void main(String[] args) {
    String eventId = args.length > 0 ? args[0] : "evt_001";

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    // A Standalone Activity is started from an ActivityClient, not a Workflow.
    ActivityClient client =
        ActivityClient.newInstance(
            service, ActivityClientOptions.newBuilder().setNamespace("default").build());

    Map<String, Object> payload = new HashMap<>();
    payload.put("eventId", eventId);
    payload.put("type", "order.created");
    payload.put("amount", 99.99);
    WebhookDelivery req = new WebhookDelivery(Shared.WEBHOOK_RECEIVER_URL, payload, eventId);

    StartActivityOptions options =
        StartActivityOptions.newBuilder()
            .setId("deliver-" + eventId)
            .setTaskQueue(Shared.TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build();

    // execute() durably enqueues the Activity as a top-level job and blocks for the result.
    // Persisted before acknowledgment, retried on failure, addressable in the UI.
    int status = client.execute(WebhookActivities.class, WebhookActivities::deliverWebhook, options, req);

    System.out.println("Standalone Activity completed with status " + status);
    System.exit(0);
  }
}
