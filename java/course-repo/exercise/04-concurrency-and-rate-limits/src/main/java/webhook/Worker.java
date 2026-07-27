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

        WorkerOptions options = WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(10)
                // TODO: cap how many Activities this Worker dispatches per second so a fan-out
                // does not overwhelm the downstream service. Add:
                //   .setMaxWorkerActivitiesPerSecond(2)
                .build();
        var worker = factory.newWorker(Shared.TASK_QUEUE, options);
        worker.registerActivitiesImplementations(new WebhookActivitiesImpl());

        factory.start();
        System.out.println("Worker running on task queue \"" + Shared.TASK_QUEUE + "\"");
        Thread.currentThread().join();
    }
}
