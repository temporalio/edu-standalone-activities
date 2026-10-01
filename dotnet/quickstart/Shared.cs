namespace Quickstart;

public static class Shared
{
    // The Task Queue the Worker polls and the client targets. Keep this in sync
    // between the Worker and the client.
    public static readonly string TaskQueue =
        System.Environment.GetEnvironmentVariable("TEMPORAL_TASK_QUEUE")
        ?? "quickstart-standalone-activities";
}
