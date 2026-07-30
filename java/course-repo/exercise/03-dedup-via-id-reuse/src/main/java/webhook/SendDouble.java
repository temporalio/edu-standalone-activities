package webhook;

import io.temporal.api.enums.v1.ActivityIdConflictPolicy;
import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.ActivityHandle;
import io.temporal.client.StartActivityOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendDouble {
    private static final Logger log = LoggerFactory.getLogger(SendDouble.class);

    static ActivityHandle<Integer> start(ActivityClient client, String eventId, String label) {
        log.info("[{}] start activityId=deliver-{}", label, eventId);
        try {
            StartActivityOptions options = StartActivityOptions.newBuilder()
                    .setId("deliver-" + eventId)
                    .setTaskQueue(Webhook.TASK_QUEUE)
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    // TODO: add
                    //   .setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)
                    //   so the second submit returns the existing handle instead of erroring.
                    //   The import is already at the top of this file.
                    .build();
            ActivityHandle<Integer> handle = client.start(
                    WebhookActivities.class, WebhookActivities::deliverWebhook, options,
                    new WebhookDelivery(Webhook.RECEIVER_URL,
                            Map.of("eventId", eventId, "type", "dup_test"), eventId));
            log.info("[{}] handle ok (activityId=deliver-{})", label, eventId);
            return handle;
        } catch (Exception e) {
            log.error("[{}] FAILED: {}", label, e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        String eventId = args.length > 0 ? args[0] : "evt_dup";

        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        ActivityClient client = ActivityClient.newInstance(
                service, ActivityClientOptions.newBuilder().setNamespace("default").build());

        ActivityHandle<Integer> firstHandle = start(client, eventId, "call-1");
        ActivityHandle<Integer> secondHandle = start(client, eventId, "call-2");
        if (firstHandle != null) {
            firstHandle.getResult();
            log.info("[call-1] activity completed");
        }
        if (secondHandle != null) {
            secondHandle.getResult();
            log.info("[call-2] activity completed");
        }
    }
}
