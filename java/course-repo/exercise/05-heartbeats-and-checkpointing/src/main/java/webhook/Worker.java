package webhook;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import java.time.Duration;

public class Worker {
    public static void main(String[] args) throws InterruptedException {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // By default the SDK throttles heartbeats (up to ~0.8 * heartbeatTimeout between
        // flushes), so the stored checkpoint can lag behind the work actually done. For this
        // short demo we flush every second so the checkpoint keeps pace and the resume lands
        // cleanly. In production the default throttling is usually what you want.
        WorkerOptions options = WorkerOptions.newBuilder()
                .setMaxHeartbeatThrottleInterval(Duration.ofSeconds(1))
                .setDefaultHeartbeatThrottleInterval(Duration.ofSeconds(1))
                .build();
        var worker = factory.newWorker(Shared.TASK_QUEUE, options);
        worker.registerActivitiesImplementations(new WebhookActivitiesImpl());

        factory.start();
        System.out.println("Worker running on task queue \"" + Shared.TASK_QUEUE + "\"");
        Thread.currentThread().join();
    }
}
