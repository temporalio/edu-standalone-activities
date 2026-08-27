namespace Quickstart;

using Temporalio.Client;
using Temporalio.Common.EnvConfig;
using Temporalio.Worker;

public static class WorkerProgram
{
    public static async Task RunAsync()
    {
        // LoadClientConnectOptions reads the TEMPORAL_PROFILE env var (default
        // "default"), loads that profile from the TOML config file (default OS
        // config dir, or TEMPORAL_CONFIG_FILE), and applies any TEMPORAL_*
        // overrides. No connection details are hardcoded, so the same Worker runs
        // against a local dev server or Temporal Cloud.
        var connectOptions = ClientEnvConfig.LoadClientConnectOptions();
        // With no config file and no env overrides, TargetHost is null; fall back
        // to the local dev server address.
        connectOptions.TargetHost ??= "localhost:7233";

        var client = await TemporalClient.ConnectAsync(connectOptions);

        // Cancellation token so Ctrl+C shuts the Worker down cleanly.
        using var tokenSource = new CancellationTokenSource();
        Console.CancelKeyPress += (_, eventArgs) =>
        {
            tokenSource.Cancel();
            eventArgs.Cancel = true;
        };

        using var worker = new TemporalWorker(
            client,
            new TemporalWorkerOptions(Shared.TaskQueue).AddActivity(Activities.GreetAsync));

        Console.WriteLine(
            $"Worker connected to {connectOptions.TargetHost} " +
            $"(namespace: {connectOptions.Namespace ?? "default"}), " +
            $"polling task queue '{Shared.TaskQueue}'");

        try
        {
            await worker.ExecuteAsync(tokenSource.Token);
        }
        catch (OperationCanceledException)
        {
            Console.WriteLine("Worker shutting down");
        }
    }
}
