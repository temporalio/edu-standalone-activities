package webhook;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

public class Worker {
    public static void main(String[] args) throws InterruptedException {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Cap dispatch rate so we don't 429 the downstream receiver.
        // Excess work waits in the Task Queue on the server.
        WorkerOptions options = WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(10)
                .setMaxWorkerActivitiesPerSecond(2)
                .build();
        var worker = factory.newWorker(Shared.TASK_QUEUE, options);
        worker.registerActivitiesImplementations(new WebhookActivitiesImpl());

        factory.start();
        System.out.println("Worker running on task queue \"" + Shared.TASK_QUEUE + "\" (rate cap: 2/sec)");
        Thread.currentThread().join();
    }
}
