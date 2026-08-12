package quickstart;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker {
    private static final Logger log = LoggerFactory.getLogger(Worker.class);

    public static void main(String[] args) throws Exception {
        // ClientConfigProfile.load() reads the TEMPORAL_PROFILE env var (default
        // "default"), loads that profile from the TOML config file (default OS
        // config dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_*
        // overrides. No connection details are hardcoded, so the same Worker runs
        // against a local dev server or Temporal Cloud.
        ClientConfigProfile profile = ClientConfigProfile.load();
        WorkflowServiceStubsOptions serviceOptions = profile.toWorkflowServiceStubsOptions();
        WorkflowClientOptions clientOptions = profile.toWorkflowClientOptions();

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(serviceOptions);
        WorkflowClient client = WorkflowClient.newInstance(service, clientOptions);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // An Activity-only Worker: no Workflows are registered.
        var worker = factory.newWorker(Greeting.TASK_QUEUE);
        worker.registerActivitiesImplementations(new GreetActivitiesImpl());

        factory.start();
        log.info(
                "Worker connected to \"{}\" (namespace \"{}\"), polling task queue \"{}\"",
                serviceOptions.getTarget(),
                clientOptions.getNamespace(),
                Greeting.TASK_QUEUE);
        // factory.start() is non-blocking. The SDK poller threads keep this process
        // alive until you stop it, so main() has nothing left to do.
    }
}
