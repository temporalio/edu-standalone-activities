namespace Quickstart;

using Temporalio.Client;
using Temporalio.Common.EnvConfig;

public static class ClientProgram
{
    public static async Task RunAsync(string name)
    {
        // Same environment-configuration lookup the Worker uses: TEMPORAL_PROFILE
        // selects the profile, the TOML file supplies address/namespace/credentials,
        // and TEMPORAL_* env vars can override individual fields.
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        connectOptions.TargetHost ??= "localhost:7233";

        var client = await TemporalClient.ConnectAsync(connectOptions);

        Console.WriteLine(
            $"Executing Standalone Activity against {connectOptions.TargetHost} " +
            $"(namespace: {connectOptions.Namespace ?? "default"})...");

        // ExecuteActivityAsync starts a Standalone Activity: no Workflow is
        // involved. It durably enqueues the Activity, waits for a Worker to run
        // it, and returns its result.
        var result = await client.ExecuteActivityAsync(
            () => Activities.GreetAsync(name),
            new StartActivityOptions(
                $"greet-{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}",
                Shared.TaskQueue)
            {
                StartToCloseTimeout = TimeSpan.FromSeconds(10),
            });

        Console.WriteLine($"Standalone Activity result: {result}");
    }
}
