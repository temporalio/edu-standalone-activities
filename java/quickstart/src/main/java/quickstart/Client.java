package quickstart;

import io.temporal.client.ActivityClient;
import io.temporal.client.ActivityClientOptions;
import io.temporal.client.StartActivityOptions;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.envconfig.ClientConfigProfile;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) throws Exception {
        String name = args.length > 0 ? args[0] : "Temporal";

        // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
        // selects the profile, the TOML file supplies address/namespace/credentials,
        // and TEMPORAL_* env vars can override individual fields.
        ClientConfigProfile profile = ClientConfigProfile.load();
        WorkflowServiceStubsOptions serviceOptions = profile.toWorkflowServiceStubsOptions();
        WorkflowClientOptions clientOptions = profile.toWorkflowClientOptions();

        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(serviceOptions);
        ActivityClient client =
                ActivityClient.newInstance(
                        service,
                        ActivityClientOptions.newBuilder()
                                .setNamespace(clientOptions.getNamespace())
                                .build());

        log.info(
                "Executing Standalone Activity against \"{}\" (namespace \"{}\")...",
                serviceOptions.getTarget(),
                clientOptions.getNamespace());

        // One API call submits a Standalone Activity: no Workflow is involved.
        StartActivityOptions options =
                StartActivityOptions.newBuilder()
                        .setId("greet-" + System.nanoTime())
                        .setTaskQueue(Greeting.TASK_QUEUE)
                        .setStartToCloseTimeout(Duration.ofSeconds(10))
                        .build();

        // execute blocks until the Activity completes and returns its result.
        String result = client.execute(GreetActivities.class, GreetActivities::greet, options, name);
        log.info("Standalone Activity result: {}", result);

        service.shutdown();
    }
}
