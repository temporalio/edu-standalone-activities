package webhooks;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * The Worker program. It polls the Task Queue for Activity tasks (and Workflow
 * tasks) and runs them. The registration below is identical to a Worker used
 * only for Workflows. A Standalone Activity is served by the same Worker.
 */
public class Worker {
  public static void main(String[] args) throws InterruptedException {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    io.temporal.worker.Worker worker = factory.newWorker(Shared.TASK_QUEUE);
    worker.registerActivitiesImplementations(new WebhookActivitiesImpl());
    worker.registerWorkflowImplementationTypes(WebhookWorkflowImpl.class);

    factory.start();
    System.out.println("Worker running on task queue '" + Shared.TASK_QUEUE + "'");

    // Poller threads run in the background; block the main thread so the JVM stays up.
    new CountDownLatch(1).await();
  }
}
