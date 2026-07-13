package webhooks;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * The Worker program. It polls the Task Queue and runs the batch delivery
 * Activity. A Standalone Activity is served by the same Worker you would use
 * for Workflows.
 */
public class Worker {
  public static void main(String[] args) throws InterruptedException {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    // Flush Activity heartbeats about once a second so the checkpoint a retry
    // reads is current. By default the SDK throttles heartbeats to roughly 80%
    // of the heartbeat timeout, which would leave the stored checkpoint several
    // items stale and make the resume redo work it had already done.
    WorkerOptions options =
        WorkerOptions.newBuilder()
            .setMaxHeartbeatThrottleInterval(Duration.ofSeconds(1))
            .setDefaultHeartbeatThrottleInterval(Duration.ofSeconds(1))
            .build();

    io.temporal.worker.Worker worker = factory.newWorker(Shared.TASK_QUEUE, options);
    worker.registerActivitiesImplementations(new WebhookActivitiesImpl());

    factory.start();
    System.out.println("Worker running on task queue '" + Shared.TASK_QUEUE + "'");

    // Poller threads run in the background; block the main thread so the JVM stays up.
    new CountDownLatch(1).await();
  }
}
