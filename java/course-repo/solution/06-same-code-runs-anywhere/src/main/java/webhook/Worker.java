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
        // This module also registers the Workflow so Module 06's upgrade path works from the same Worker.
        worker.registerWorkflowImplementationTypes(WebhookWorkflowImpl.class);

        factory.start();
        System.out.println("Worker running on task queue \"" + Shared.TASK_QUEUE + "\"");
        // Keep the process alive so the Worker keeps polling until you stop it.
        Thread.currentThread().join();
    }
}
