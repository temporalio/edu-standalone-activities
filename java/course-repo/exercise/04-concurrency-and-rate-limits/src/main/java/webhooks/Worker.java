package webhooks;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;

import java.util.concurrent.CountDownLatch;

/**
 * The Worker program. It polls the Task Queue for Activity tasks and runs them.
 * A Standalone Activity is served by the same kind of Worker you would use for
 * Workflows.
 */
public class Worker {
  public static void main(String[] args) throws InterruptedException {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    WorkerOptions workerOptions =
        WorkerOptions.newBuilder()
            .setMaxConcurrentActivityExecutionSize(10)
            // TODO: cap how many Activities this Worker dispatches per second so
            // we don't overwhelm the downstream service.
            // The WorkerOptions method is: setMaxTaskQueueActivitiesPerSecond(<number>)
            // Try 2 first to match the receiver's 2 req/sec rate limit.
            .build();

    io.temporal.worker.Worker worker = factory.newWorker(Shared.TASK_QUEUE, workerOptions);
    worker.registerActivitiesImplementations(new WebhookActivitiesImpl());

    factory.start();
    System.out.println("Worker running on task queue '" + Shared.TASK_QUEUE + "'");

    // Poller threads run in the background; block the main thread so the JVM stays up.
    new CountDownLatch(1).await();
  }
}
