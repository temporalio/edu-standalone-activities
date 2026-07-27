package webhook;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;

public class Worker {
    public static void main(String[] args) throws InterruptedException {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        var worker = factory.newWorker(Shared.TASK_QUEUE);
        worker.registerActivitiesImplementations(new WebhookActivitiesImpl());
        // Also register the Workflow so the same Worker serves both job types (Module 06).
        worker.registerWorkflowImplementationTypes(WebhookWorkflowImpl.class);

        factory.start();
        System.out.println("Worker running on task queue \"" + Shared.TASK_QUEUE + "\"");
        Thread.currentThread().join(); // keep polling until stopped
    }
}
