namespace Quickstart;

public static class Shared
{
    // The Task Queue the Worker polls and the client targets. Keep this in sync
    // between the Worker and the client. Reads TEMPORAL_TASK_QUEUE when set — e.g. a
    // test harness isolating each run on its own queue — else the shared default, so a
    // copy-paste user's behavior is unchanged.
    public static readonly string TaskQueue =
        System.Environment.GetEnvironmentVariable("TEMPORAL_TASK_QUEUE")
        ?? "quickstart-standalone-activities";
}
