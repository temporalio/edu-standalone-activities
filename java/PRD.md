# Course Outline: Standalone Activities as a Durable Job Queue (Java)

> **Status:** track definition complete, image build pending.

## In one sentence

Build a durable webhook-delivery service with Standalone Activities - Temporal's durable job queue - in Java, and learn how the platform replaces the broker, scheduler, retry library, and result store you'd otherwise have to operate yourself.

## Audience

Java developers who already understand Temporal Activities.

## Why this course

Port of the [Python standalone activities tutorial](https://github.com/temporalio/edu-standalone-activities/tree/main/python) to Java, matching the [TypeScript edition](https://github.com/temporalio/edu-standalone-activities/tree/main/typescript) module-for-module. Same six-module narrative, same webhook delivery use case, Java SDK idioms throughout.

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
│   └── Dockerfile         # Container image baked with Temurin 21, Gradle, Temporal CLI, python3
└── course-repo/           # Java exercise code baked into the container
    ├── scripts/           # kill-worker, reset-receiver, restart-worker, stop-demo-and-reset
    ├── demos/             # Interactive HTML diagrams served on :9001
    ├── exercise/
    │   └── 01-06-*/       # Per-module Gradle project, starter code with TODO markers
    └── solution/
        └── 01-06-*/       # Per-module Gradle project, completed code

shared/
└── webhook-receiver/
    └── webhook_receiver.py  # SDK-agnostic Python stdlib receiver, shared across editions
```

## Modules

| # | Name | Goal | Fail-then-fix |
|---|------|------|---------------|
| 01 | **Durable job queue** | Write `deliverWebhook`, submit via `ActivityClient.execute`, inspect in UI. | Introduction, no fail-then-fix. |
| 02 | **Idempotency and crash safety** | Make retries safe with an `Idempotency-Key` header. | Reproduce 3 POSTs for 1 event; add one header; receiver dedupes. |
| 03 | **Dedup via ID reuse** | Use `ActivityIdConflictPolicy.USE_EXISTING` to reject duplicate starts. | Second `start()` with same id errors; add policy; both succeed. |
| 04 | **Concurrency and rate limits** | Cap Worker throughput with `maxWorkerActivitiesPerSecond`. | Unbounded fan-out floods receiver with 429s; add rate cap; 429s stop. |
| 05 | **Heartbeats and checkpointing** | Read `getHeartbeatDetails` on retry to resume from checkpoint. | Kill Worker mid-batch; retry restarts from 0; add checkpoint read; resumes cleanly. |
| 06 | **Same code runs anywhere** | Same `deliverWebhook` submitted standalone and as a Workflow step. | Capstone; no fail-then-fix. |

## Java SDK API notes (verified against the Temporal Docs, SDK 1.36.1, Public Preview)

- `ActivityClient.newInstance(service, ActivityClientOptions.newBuilder().setNamespace("default").build())` - the Standalone Activity client.
- `client.execute(Iface.class, Iface::method, options, args...)` - submit and block for the typed result.
- `client.start(Iface.class, Iface::method, options, args...)` - submit and get an `ActivityHandle<R>`; `handle.getResult()` / `handle.getResultAsync()` fetch the result.
- `StartActivityOptions.newBuilder().setId(...).setTaskQueue(...).setStartToCloseTimeout(...)` - requires id, taskQueue, and one of startToClose / scheduleToClose.
  - `.setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)` (Module 03)
  - `.setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(5).build())` (Module 02)
  - `.setHeartbeatTimeout(Duration.ofSeconds(5))` (Module 05)
- `Activity.getExecutionContext()` -> `ctx.getInfo().getAttempt()`, `ctx.heartbeat(value)`, `ctx.getHeartbeatDetails(Integer.class)` (returns `Optional`).
- `ApplicationFailure.newFailure(message, type)` (Module 02).
- `WorkerOptions.newBuilder().setMaxWorkerActivitiesPerSecond(2)` per-worker cap; `.setMaxTaskQueueActivitiesPerSecond(2)` queue-wide cap (Module 04).
- `Workflow.newActivityStub(Iface.class, ActivityOptions...)` to call the same Activity from a Workflow (Module 06).

## Running in the sandbox

- The course-repo is baked into the image at `/opt/workshop` and pre-built at image-build time.
- Each module is its own self-contained Gradle project (`build.gradle` + `settings.gradle`).
- Attendees run any `main()` with the documented Standalone Activities pattern:
  - Worker: `gradle -q execute -PmainClass=webhook.Worker`
  - Starter: `gradle -q execute -PmainClass=webhook.SendStandalone -PappArgs=evt_001`

## Deviation from the TypeScript edition: tab order

The TypeScript track (PR #52) was authored before the repo standardized on
**Temporal UI as the first / landing tab** (PRs #53, #55, now codified in
`AGENTS.md`). This Java edition follows the current canonical tab order
(Temporal UI first) rather than the TypeScript track's older ordering, so it
matches the Python reference and every merged track. Everything else, the
exercises, prose, module narrative, and interactive demos, mirrors the
TypeScript edition.

## First push checklist

1. Build and push the sandbox image (build context is the repo root):
   `docker buildx build --platform linux/amd64 -f java/sandbox/Dockerfile -t ghcr.io/temporalio/edu-standalone-activities-sandbox:java-devrel-skill-latest --push .`
2. Create the Instruqt track slug: `instruqt track create standalone-activities-java --title "Build a Job Queue with Standalone Activities (Java)"`.
3. Push the track: `cd java/instruqt && instruqt track push --force`.
4. Pull the server-assigned ids: `instruqt track pull`.
5. Commit the populated `track.yml` and `assignment.md` ids.
