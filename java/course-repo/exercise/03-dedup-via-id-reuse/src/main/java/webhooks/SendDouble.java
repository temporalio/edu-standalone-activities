package webhooks;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityHandle;
import io.temporal.client.StartActivityOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Calls start() TWICE with the same Activity ID, back-to-back.
 *
 * Simulates an upstream system sending the same logical event twice.
 * Without an ID conflict policy, the second call raises an error because the
 * default conflict policy is FAIL.
 *
 * Fix: set the conflict policy to USE_EXISTING on the StartActivityOptions so
 * the second call quietly returns a handle to the existing run.
 */
public class SendDouble {

  static ActivityHandle<Integer> start(
      ActivityClient client, String eventId, String label) {
    System.out.println("[" + label + "] start activityId=deliver-" + eventId);

    Map<String, Object> payload = new HashMap<>();
    payload.put("eventId", eventId);
    payload.put("type", "dup_test");
    WebhookDelivery req = new WebhookDelivery(Shared.WEBHOOK_RECEIVER_URL, payload, eventId);

    StartActivityOptions options =
        StartActivityOptions.newBuilder()
            .setId("deliver-" + eventId)
            .setTaskQueue(Shared.TASK_QUEUE)
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            // TODO: set the conflict policy to USE_EXISTING here so the second
            // call returns the existing handle instead of erroring. For example:
            // .setIdConflictPolicy(
            //     io.temporal.api.enums.v1.ActivityIdConflictPolicy
            //         .ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)
            .build();

    try {
      ActivityHandle<Integer> handle =
          client.start(WebhookActivities.class, WebhookActivities::deliverWebhook, options, req);
      System.out.println("[" + label + "] handle ok (activityId=deliver-" + eventId + ")");
      return handle;
    } catch (Exception err) {
      // The default FAIL policy makes the second start throw
      // io.temporal.client.ActivityAlreadyStartedException.
      System.out.println("[" + label + "] FAILED: " + err);
      return null;
    }
  }

  public static void main(String[] args) {
    String eventId = args.length > 0 ? args[0] : "evt_dup";

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    ActivityClient client =
        ActivityClient.newInstance(
            service, ActivityClientOptions.newBuilder().setNamespace("default").build());

    ActivityHandle<Integer> h1 = start(client, eventId, "call-1");
    // The second call simulates the upstream retry/replay arriving right after the first.
    ActivityHandle<Integer> h2 = start(client, eventId, "call-2");

    if (h1 != null) {
      h1.getResult();
      System.out.println("[call-1] activity completed");
    }
    if (h2 != null) {
      h2.getResult();
      System.out.println("[call-2] activity completed");
    }

    System.exit(0);
  }
}
