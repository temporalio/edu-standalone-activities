package webhook;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        var worker = factory.newWorker(Webhook.TASK_QUEUE);
        worker.registerActivitiesImplementations(new WebhookActivitiesImpl());
        // This module also registers the Workflow so Module 06's upgrade path works from the same Worker.
        worker.registerWorkflowImplementationTypes(WebhookWorkflowImpl.class);

        factory.start();
        log.info("Worker running on task queue \"{}\"", Webhook.TASK_QUEUE);
        // factory.start() is non-blocking. The SDK poller threads keep this process
        // alive until you stop it, so main() has nothing left to do.
    }
}
