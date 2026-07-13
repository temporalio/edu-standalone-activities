# Course Outline: Standalone Activities as a Durable Job Queue (Java)

> **Status:** track definition complete, image build pending.

## In one sentence

Build a durable webhook-delivery service with Standalone Activities - Temporal's durable job queue - in Java, and learn how the platform replaces the broker, scheduler, retry library, and result store you'd otherwise have to operate yourself.

## Audience

Java developers who already understand Temporal Activities.

## Why this course

Port of the [Python standalone activities tutorial](https://github.com/temporalio/edu-standalone-activities/tree/main/python) and its [TypeScript sibling](https://github.com/temporalio/edu-standalone-activities/tree/main/typescript) to Java. Same six-module narrative, same webhook delivery use case, Java SDK idioms throughout (interface + implementation, Maven, `ActivityClient`).

## Layout

```
java/
├── PRD.md                 # This file
├── README.md
├── instruqt/              # Instruqt track definition
│   ├── track.yml
│   ├── config.yml
│   ├── track_scripts/
│   │   ├── setup-workshop
│   │   └── cleanup-workshop
│   └── 01-06-*/           # Per-challenge: assignment.md, setup/check/solve-workshop
├── sandbox/
│   └── Dockerfile         # Container image baked with Java 21, Maven, Temporal CLI
└── course-repo/           # Java exercise code baked into the container
    ├── pom.xml            # Parent POM + aggregator
    ├── server/            # Webhook receiver + demo file server (JDK HttpServer + Jackson)
    ├── scripts/           # kill-worker / restart-worker / reset-receiver / stop-demo-and-reset
    ├── demos/             # Standalone HTML demos (Module 05)
    ├── exercise/
    │   └── 01-06-*/src/main/java/webhooks/*.java    # Starter code with TODO markers
    └── solution/
        └── 01-06-*/src/main/java/webhooks/*.java    # Completed code
```

## Modules

| # | Name | Goal | Fail-then-fix |
|---|------|------|---------------|
| 01 | **Durable job queue** | Write `deliverWebhook`, submit via `ActivityClient.execute`, inspect in UI. | Introduction, no fail-then-fix. |
| 02 | **Idempotency and crash safety** | Make retries safe with an `Idempotency-Key` header. | Reproduce 3 POSTs for 1 event; add one header; receiver dedupes. |
| 03 | **Dedup via ID reuse** | Use `ActivityIdConflictPolicy` USE_EXISTING to reject duplicate starts. | Second `start()` with same id errors; add policy; both succeed. |
| 04 | **Concurrency and rate limits** | Cap Worker throughput with `setMaxTaskQueueActivitiesPerSecond`. | Unbounded fan-out floods receiver with 429s; add rate cap; 429s stop. |
| 05 | **Heartbeats and checkpointing** | Read `getHeartbeatDetails` on retry to resume from checkpoint. | Take Worker down mid-batch; retry restarts from 0; add checkpoint read; resumes cleanly. |
| 06 | **Same code runs anywhere** | Same `deliverWebhook` submitted standalone and as a Workflow step. | Capstone; no fail-then-fix. |

## Java SDK API notes

- `ActivityClient.newInstance(service, ActivityClientOptions...)` - the client for Standalone Activities
- `client.execute(Iface.class, Iface::method, StartActivityOptions, args...)` - submit + wait
- `client.start(...)` - submit, returns `ActivityHandle<T>`; `handle.getResult()` waits
- `StartActivityOptions.newBuilder().setId(...).setTaskQueue(...).setStartToCloseTimeout(...)` (also `setHeartbeatTimeout`, `setIdConflictPolicy`, `setRetryOptions`)
- `ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING` (Module 03)
- `WorkerOptions.newBuilder().setMaxTaskQueueActivitiesPerSecond(double)` (Module 04)
- `Activity.getExecutionContext().heartbeat(details)` and `.getHeartbeatDetails(Class)` (Module 05)
- `Workflow.newActivityStub(...)` for the Workflow step (Module 06)

The Activity **type name** is capitalized in the UI/CLI (`deliverWebhook` method → `DeliverWebhook`); the Activity **ID** is `deliver-<eventId>`.

## Module resolution in the sandbox

- The parent POM is installed into `/root/.m2` at image-build time (`mvn -N install`), and all dependencies are pre-downloaded (`mvn compile` across the reactor).
- Each seeded module directory carries its own `pom.xml` that inherits from the parent by coordinates, so `mvn` resolves it from `/root/.m2` regardless of where the directory is copied.
- Attendees run: `mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker` and `... -Dexec.mainClass=webhooks.SendStandalone -Dexec.args="evt_001"`.

## First push checklist

1. Build and push the sandbox image: `docker buildx build --platform linux/amd64 -t ghcr.io/temporalio/edu-standalone-activities-sandbox:java-instruqt-skill-latest --push java/` (build context is `java/`).
2. Make the GHCR package public.
3. `cd java/instruqt && instruqt track validate`.
4. `instruqt track push` (first push writes the track `id` and per-challenge/tab `id`s back into the files; commit them).

## Open questions

1. Confirm the exact `StartActivityOptions.Builder` setter names in the pinned SDK for heartbeat timeout and retry options during end-to-end testing.
2. The `docs/java-idempotency-demo` iframe URL is branch-pinned via raw.githack; swap the branch segment to `main` on merge (AGENTS.md).
3. Pin `io.temporal:temporal-sdk` to a specific version once Standalone Activities reach GA.
