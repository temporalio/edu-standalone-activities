# Course Outline: Standalone Activities as a Durable Job Queue (Go)

> **Status:** track definition complete, image build pending.

## In one sentence

Build a durable webhook delivery service with Standalone Activities, Temporal's durable job queue, in Go, and learn how the platform replaces the broker, scheduler, retry library, and result store you'd otherwise have to operate yourself.

## Audience

Go developers who already understand Temporal Activities.

## Why this course

Port of the [Python standalone activities tutorial](https://github.com/temporalio/edu-standalone-activities/tree/main/python) to Go. Same six-module narrative, same webhook delivery use case, Go SDK idioms throughout.

## Layout

```
go/
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
│   └── Dockerfile         # Container image baked with Go, the Temporal CLI, and a pre-warmed module cache
└── course-repo/           # Go exercise code baked into the container
    ├── go.mod              # Single module: standaloneactivities
    ├── go.sum
    ├── server/
    │   ├── webhookreceiver/main.go   # Port of the Python stdlib webhook receiver
    │   └── demoserver/main.go        # Static file server for the HTML demos
    ├── scripts/
    │   ├── kill-worker.sh
    │   ├── reset-receiver.sh
    │   ├── restart-worker.sh
    │   └── stop-demo-and-reset.sh
    ├── demos/
    │   ├── heartbeat-demo/index.html
    │   └── heartbeat-topology/index.html
    ├── exercise/
    │   └── 01-06-*/{webhook,worker,sendstandalone,...}/*.go   # Starter code with TODO markers
    └── solution/
        └── 01-06-*/{webhook,worker,sendstandalone,...}/*.go   # Completed code
```

## Modules

| # | Name | Goal | Fail-then-fix |
|---|------|------|---------------|
| 01 | **Durable job queue** | Write `DeliverWebhook`, submit via `client.ExecuteActivity`, inspect it in the UI. | Introduction, no fail-then-fix. |
| 02 | **Idempotency and crash safety** | Make retries safe with an `Idempotency-Key` header. | Reproduce 3 POSTs for 1 event, add one header, receiver dedupes. |
| 03 | **Dedup via ID reuse** | Use `ActivityIDConflictPolicy: enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING` to reject duplicate starts. | Second `ExecuteActivity` call with the same id errors, add the policy, both succeed. |
| 04 | **Concurrency and rate limits** | Cap Worker throughput with `worker.Options{WorkerActivitiesPerSecond, MaxConcurrentActivityExecutionSize}`. | Unbounded fan-out floods the receiver with 429s, add the rate cap, 429s stop. |
| 05 | **Heartbeats and checkpointing** | Read heartbeat details on retry to resume from a checkpoint. | Kill the Worker mid-batch, retry restarts from 0, add the checkpoint read, resumes cleanly. |
| 06 | **Same code runs anywhere** | Same `DeliverWebhook` submitted standalone and as a Workflow step. | Capstone, no fail-then-fix. |

## Go SDK API notes

Standalone Activities are Public Preview. The SDK is pinned to `go.temporal.io/sdk v1.45.0` (see `go/course-repo/go.mod`).

- Submit and wait: `handle, err := c.ExecuteActivity(ctx, client.StartActivityOptions{ID, TaskQueue, StartToCloseTimeout}, DeliverWebhook, req)` returns a `client.ActivityHandle`; `handle.Get(ctx, &res)` blocks for the result.
- Dedup at the platform: `client.StartActivityOptions{ActivityIDConflictPolicy: enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING}`, where `enums` is `go.temporal.io/api/enums/v1`.
- Rate and concurrency caps: `worker.Options{WorkerActivitiesPerSecond: 2, MaxConcurrentActivityExecutionSize: 10}` passed to `worker.New`.
- Heartbeats and resume: `activity.RecordHeartbeat(ctx, n)` inside the Activity; on the next attempt, `activity.HasHeartbeatDetails(ctx)` and `activity.GetHeartbeatDetails(ctx, &n)` read the last checkpoint.
- Activity as a Workflow step (Module 06): `workflow.ExecuteActivity(ctx, DeliverWebhook, req).Get(ctx, &res)`, same `DeliverWebhook` function used standalone in Module 01.

## Module resolution in sandbox

- There is a single `go.mod` at the `course-repo/` root, module `standaloneactivities`. Every exercise and solution package lives under that one module tree, so there is no per-module dependency drift to manage.
- The whole module tree is seeded to `/root/workshop` in the sandbox image.
- Dependencies are pre-warmed at image build time with `go build ./...`, so the module cache (`$GOPATH/pkg/mod`) is already populated and the first `go run` in a challenge does not pay a download tax.
- Go resolves imports from the nearest `go.mod`, so attendees can run commands from any exercise directory without extra configuration.
- Attendees run `go run ./worker` and `go run ./sendstandalone evt_001` from a module directory (for example `exercise/01-durable-job-queue/`).

## First push checklist

1. Create a GitHub repo for the track definition (this repo).
2. Build and push the sandbox image: `docker build -t ghcr.io/temporalio/edu-standalone-activities-sandbox:go-latest ./sandbox && docker push`.
3. Create the Instruqt track slug: `instruqt track create standalone-activities-go --title "Build a Job Queue with Standalone Activities (Go)"`.
4. Push the track: `cd instruqt && instruqt track push --force`.
5. Pull the server-assigned ids: `cd .. && instruqt track pull`.
6. Commit the populated `track.yml` (with ids filled in).

## Open questions

1. Confirm `enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING` is the correct constant name and import path in `go.temporal.io/api/enums/v1` for the pinned SDK version.
2. Confirm `worker.Options` field names (`WorkerActivitiesPerSecond`, `MaxConcurrentActivityExecutionSize`) match the pinned `go.temporal.io/sdk v1.45.0` release.
3. Task Queue Priority and Fairness API availability in the Go SDK (Module 04 currently only references it conceptually).
4. Re-pin `go.temporal.io/sdk` once the Standalone Activities API leaves Public Preview and reaches general availability.
