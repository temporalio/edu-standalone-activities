namespace Quickstart;

using Temporalio.Activities;

public static class Activities
{
    // A plain Activity. Nothing here knows or cares whether it was invoked as a
    // Standalone Activity or from a Workflow: the same method works either way.
    // The default Activity type name is "Greet" (the "Async" suffix is stripped).
    [Activity]
    public static Task<string> GreetAsync(string name) =>
        Task.FromResult($"Hello, {name}! This ran as a Standalone Activity.");
}
