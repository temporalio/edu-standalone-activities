# Standalone Activities as a Durable Job Queue (Java)

An Instruqt track that teaches Java developers how to use Temporal Standalone
Activities to build a durable webhook delivery service, without a Workflow,
broker, or retry library. See [PRD.md](./PRD.md) for the full course outline.

## What this track teaches

1. **Durable job queue**: submit an Activity directly with `ActivityClient.execute`,
   no Workflow required.
2. **Idempotency and crash safety**: make retries safe with an `Idempotency-Key` header.
3. **Dedup via ID reuse**: use `ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING`
   to reject duplicate submissions at the platform layer.
4. **Concurrency and rate limits**: cap Worker throughput with
   `WorkerOptions.setMaxTaskQueueActivitiesPerSecond`.
5. **Heartbeats and checkpointing**: resume a long-running Activity from its last
   checkpoint after a restart, using `Activity.getExecutionContext().heartbeat()` and
   `getHeartbeatDetails()`.
6. **Same code runs anywhere**: run the exact same Activity method standalone and as
   a step inside a Workflow.

Standalone Activities are Public Preview. This track pins the Temporal Java SDK
(`io.temporal:temporal-sdk`) to `1.36.0`, the first release where Standalone
Activity support reached Public Preview (see `course-repo/pom.xml`). It requires
Temporal CLI v1.7.0+ and Temporal Server v1.31.0+.

## Running locally

Prerequisites: Java 21+, Maven 3.9+, and the Temporal CLI. All commands assume you
are in `java/course-repo/` unless noted otherwise.

Install the parent POM once so each module can resolve it:

```bash
mvn -N install
```

Start the Temporal dev server:

```bash
temporal server start-dev
```

Start the webhook receiver and demo server (in a second terminal):

```bash
mvn -f server/pom.xml package
java -cp server/target/webhooks-server.jar webhooks.server.WebhookReceiver
```

Run the Worker for a module (in a third terminal):

```bash
cd exercise/01-durable-job-queue
mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker
```

Submit a webhook delivery as a Standalone Activity (in a fourth terminal, from the
same module directory):

```bash
mvn -q compile exec:java -Dexec.mainClass=webhooks.SendStandalone -Dexec.args="evt_001"
```

## Layout

- `course-repo/` is the Maven project baked into the sandbox image. The root
  `pom.xml` is both the parent (Temporal SDK dependency, Java 21 compiler, the
  exec plugin) and an aggregator that compiles all 12 module directories.
  `exercise/` holds starter code with `TODO` markers, `solution/` holds the
  completed code, `server/` holds the webhook receiver and demo static file
  server, `scripts/` holds the helper scripts used by the challenge check/solve
  steps, and `demos/` holds the standalone HTML demos shown during Module 05.
- `sandbox/Dockerfile` builds the image the track runs in (Java 21 + Maven +
  Temporal CLI, with the Maven repository pre-warmed).
- `instruqt/` holds the track definition (`track.yml`, `config.yml`, per-challenge
  assignment files and lifecycle scripts).

## How code runs in the sandbox

Each module directory (`exercise/NN-slug/` and `solution/NN-slug/`) is its own
small Maven project whose `pom.xml` inherits from the parent POM installed in the
image's local Maven repository (`/root/.m2`). Learners edit a `.java` file, then
run `mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker`, which recompiles
and runs in one step. Because the mainClass appears on the process command line,
`pkill -f "webhooks.Worker"` can take the Worker down for the Module 05 crash demo.

## Helper scripts

Located in `course-repo/scripts/`, used by challenge check/solve steps and available
to learners:

- `kill-worker.sh`: SIGKILLs the running Worker process to force a mid-flight crash.
- `restart-worker.sh`: relaunches the Worker in the background.
- `reset-receiver.sh`: resets the webhook receiver's counters.
- `stop-demo-and-reset.sh`: terminates any leftover demo Activities from the
  rate-limit demo, then resets the receiver.
