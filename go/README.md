# Standalone Activities as a Durable Job Queue (Go)

An Instruqt track that teaches Go developers how to use Temporal Standalone Activities
to build a durable webhook delivery service, without a Workflow, broker, or retry
library. See [PRD.md](./PRD.md) for the full course outline.

## What this track teaches

1. **Durable job queue**: submit an Activity directly with `client.ExecuteActivity`, no
   Workflow required.
2. **Idempotency and crash safety**: make retries safe with an `Idempotency-Key` header.
3. **Dedup via ID reuse**: use `ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING` to reject
   duplicate submissions at the platform layer.
4. **Concurrency and rate limits**: cap Worker throughput with `worker.Options`.
5. **Heartbeats and checkpointing**: resume a long-running Activity from its last
   checkpoint after a crash.
6. **Same code runs anywhere**: run the exact same Activity function standalone and as
   a step inside a Workflow.

Standalone Activities are Public Preview. This track pins `go.temporal.io/sdk` to
`v1.45.0` (see `course-repo/go.mod`).

## Running locally

All commands assume you are in `go/course-repo/` unless noted otherwise.

Start the Temporal dev server:

```bash
temporal server start-dev
```

Start the webhook receiver (in a second terminal):

```bash
go run ./server/webhookreceiver
```

Run the Worker for a module (in a third terminal):

```bash
cd exercise/01-durable-job-queue && go run ./worker
```

Submit a webhook delivery as a Standalone Activity (in a fourth terminal, from the same
module directory):

```bash
go run ./sendstandalone evt_001
```

## Layout

- `course-repo/` is the Go module (`standaloneactivities`) baked into the sandbox
  image: `exercise/` holds starter code with TODO markers, `solution/` holds the
  completed code, `server/` holds the webhook receiver and demo static file server,
  `scripts/` holds the helper scripts used by the challenge check/solve steps, and
  `demos/` holds the standalone HTML demos shown during Modules 05 and 06.
- `sandbox/Dockerfile` builds the image the track runs in.
- `instruqt/` holds the track definition (`track.yml`, `config.yml`, per-challenge
  assignment files and lifecycle scripts).

## Helper scripts

Located in `course-repo/scripts/`, used by challenge check/solve steps and available to
learners:

- `kill-worker.sh`: SIGKILLs the running Worker process to force a mid-flight crash.
- `restart-worker.sh`: kills the Worker, then relaunches it in the background.
- `reset-receiver.sh`: resets the webhook receiver's counters.
- `stop-demo-and-reset.sh`: terminates any leftover demo Activities from the rate-limit
  demo, then resets the receiver.
