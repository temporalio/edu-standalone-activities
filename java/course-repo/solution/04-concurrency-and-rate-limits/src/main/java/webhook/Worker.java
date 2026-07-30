package webhook;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Cap dispatch rate so we don't 429 the downstream receiver.
        // Excess work waits in the Task Queue on the server.
        WorkerOptions options = WorkerOptions.newBuilder()
                .setMaxConcurrentActivityExecutionSize(10)
                .setMaxWorkerActivitiesPerSecond(2)
                .build();
        var worker = factory.newWorker(Webhook.TASK_QUEUE, options);
        worker.registerActivitiesImplementations(new WebhookActivitiesImpl());

        factory.start();
        log.info("Worker running on task queue \"{}\" (rate cap: 2/sec)", Webhook.TASK_QUEUE);
        // factory.start() is non-blocking. The SDK poller threads keep this process
        // alive until you stop it, so main() has nothing left to do.
    }
}
