namespace Quickstart;

public static class Shared
{
    // The Task Queue the Worker polls and the client targets. Keep this in sync
    // between the Worker and the client.
    public const string TaskQueue = "quickstart-standalone-activities";
}
