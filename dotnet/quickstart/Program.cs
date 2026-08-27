using Quickstart;

// Two entry points, mirroring the Worker/client split of the other quickstarts:
//   dotnet run worker
//   dotnet run client [name]
var command = args.Length > 0 ? args[0] : string.Empty;

switch (command)
{
    case "worker":
        await WorkerProgram.RunAsync();
        break;
    case "client":
        var name = args.Length > 1 ? args[1] : "Temporal";
        await ClientProgram.RunAsync(name);
        break;
    default:
        Console.Error.WriteLine("Usage: dotnet run worker | dotnet run client [name]");
        Environment.Exit(1);
        break;
}
