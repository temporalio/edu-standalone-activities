package webhook;

import io.temporal.api.enums.v1.ActivityIdConflictPolicy;
import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityHandle;
import io.temporal.client.StartActivityOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.Map;

public class SendDouble {
    static ActivityHandle<Integer> start(ActivityClient client, String eventId, String label) {
        System.out.println("[" + label + "] start activityId=deliver-" + eventId);
        try {
            StartActivityOptions options = StartActivityOptions.newBuilder()
                    .setId("deliver-" + eventId)
                    .setTaskQueue(Shared.TASK_QUEUE)
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    // USE_EXISTING: the second submit with the same ID returns a handle to the
                    // existing Activity instead of erroring. Server-side dedup before any Worker runs it.
                    .setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)
                    .build();
            ActivityHandle<Integer> handle = client.start(
                    WebhookActivities.class, WebhookActivities::deliverWebhook, options,
                    new WebhookDelivery(Shared.WEBHOOK_RECEIVER_URL,
                            Map.of("eventId", eventId, "type", "dup_test"), eventId));
            System.out.println("[" + label + "] handle ok (activityId=deliver-" + eventId + ")");
            return handle;
        } catch (Exception e) {
            System.out.println("[" + label + "] FAILED: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        String eventId = args.length > 0 ? args[0] : "evt_dup";

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        ActivityClient client = ActivityClient.newInstance(
                service, ActivityClientOptions.newBuilder().setNamespace("default").build());

        ActivityHandle<Integer> h1 = start(client, eventId, "call-1");
        ActivityHandle<Integer> h2 = start(client, eventId, "call-2");
        if (h1 != null) {
            h1.getResult();
            System.out.println("[call-1] activity completed");
        }
        if (h2 != null) {
            h2.getResult();
            System.out.println("[call-2] activity completed");
        }
    }
}
