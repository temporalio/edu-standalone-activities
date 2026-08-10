# Standalone Activities quickstart (Java)

A minimal, self-contained sample that runs a Temporal **Standalone Activity**
and reads its connection settings from Temporal's environment-configuration
feature. The same Worker and client run against a local dev server or Temporal
Cloud with no code changes: you switch environments by selecting a profile.

## What's here

| File | Purpose |
|---|---|
| `GreetActivities.java` / `GreetActivitiesImpl.java` | A plain `greet` Activity (interface + implementation). |
| `Greeting.java` | Shared Task Queue name. |
| `Worker.java` | Worker that connects via `ClientConfigProfile.load()` and polls the Task Queue. |
| `Client.java` | Executes `greet` as a Standalone Activity (`ActivityClient.execute`). |
| `config.toml` | Reference profiles (`default` for local, `cloud` for Temporal Cloud). |

Connection settings are never hardcoded. `ClientConfigProfile.load()` (from
`io.temporal:temporal-envconfig`) resolves them at startup and converts to
`WorkflowServiceStubsOptions` + `WorkflowClientOptions`. It reads:

1. the `TEMPORAL_PROFILE` env var (which profile to load, default `"default"`),
2. the TOML config file (the OS-specific default location below, or the path in
   `TEMPORAL_CONFIG_FILE`), and
3. any `TEMPORAL_*` override vars (`TEMPORAL_ADDRESS`, `TEMPORAL_NAMESPACE`,
   `TEMPORAL_API_KEY`, and so on).

## Prerequisites

- JDK 21 or later.
- Gradle (or a Gradle wrapper). Commands below use `gradle`.

## Run against a local dev server

Start a local server, then run the Worker and client. With no
`TEMPORAL_PROFILE` set, the profile name defaults to `default`; with no config
file present, the address defaults to `localhost:7233`.

```bash
temporal server start-dev          # terminal 1
gradle -q runWorker                # terminal 2
gradle -q runClient                # terminal 3
```

Expected client output:

```
Executing Standalone Activity against "localhost:7233" (namespace "default")...
Standalone Activity result: Hello, Temporal! This ran as a Standalone Activity.
```

Pass a name as an argument: `gradle -q runClient -PappArgs="Angela"`.

## Run against Temporal Cloud

1. Create the config file at the default location for your OS (copy
   `config.toml` from this directory as a starting point):

   | OS | Default config file |
   |---|---|
   | macOS | `~/Library/Application Support/temporalio/temporal.toml` |
   | Linux | `~/.config/temporalio/temporal.toml` |
   | Windows | `%APPDATA%\temporalio\temporal.toml` |

   Fill in your Temporal Cloud endpoint and namespace under `[profile.cloud]`.
   Use whichever auth method your namespace was created with (this is fixed at
   namespace creation and cannot be changed later).

   API key auth:

   ```toml
   [profile.cloud]
   address = "your-namespace.a1b2c.tmprl.cloud:7233"
   namespace = "your-namespace.a1b2c"
   api_key = "your-api-key-here"
   ```

   mTLS auth (no `api_key`; `TEMPORAL_API_KEY` must stay unset, or the SDK
   switches to API-key mode and the server rejects the connection with
   `tls: certificate required`):

   ```toml
   [profile.cloud]
   address = "your-namespace.a1b2c.tmprl.cloud:7233"
   namespace = "your-namespace.a1b2c"

   [profile.cloud.tls]
   client_cert_path = "/path/to/client.pem"
   client_key_path = "/path/to/client.key"
   ```

2. Start the Worker and run the client with the `cloud` profile selected:

   ```bash
   TEMPORAL_PROFILE=cloud gradle -q runWorker      # terminal 1
   TEMPORAL_PROFILE=cloud gradle -q runClient      # terminal 2
   ```

To keep the profiles in this repo instead of the OS config directory, point
`TEMPORAL_CONFIG_FILE` at the bundled file:

```bash
TEMPORAL_CONFIG_FILE=./config.toml TEMPORAL_PROFILE=cloud gradle -q runClient
```

If `TEMPORAL_PROFILE` names a profile that the config file does not define, the
SDK fails fast. The most common cause is the file being in the wrong place: on
macOS the default is `~/Library/Application Support/temporalio/temporal.toml`,
not `~/.config`. Put the file at the OS default above, or point
`TEMPORAL_CONFIG_FILE` at it.
