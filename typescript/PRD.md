# Course Outline: Standalone Activities as a Durable Job Queue (TypeScript)

> **Status:** track definition complete, image build pending.

## In one sentence

Build a durable webhook-delivery service with Standalone Activities - Temporal's durable job queue - in TypeScript, and learn how the platform replaces the broker, scheduler, retry library, and result store you'd otherwise have to operate yourself.

## Audience

TypeScript developers who already understand Temporal Activities.

## Why this course

Port of the [Python standalone activities tutorial](https://github.com/temporalio/edu-standalone-activities/tree/main/python) to TypeScript. Same six-module narrative, same webhook delivery use case, TypeScript SDK idioms throughout.

## Layout

```
ts/
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
│   └── Dockerfile         # Container image baked with Node.js 22, ts-node, Temporal CLI
└── course-repo/           # TypeScript exercise code baked into the container
    ├── package.json
    ├── tsconfig.json
    ├── server/
    │   └── webhookReceiver.ts   # Port of Python stdlib webhook receiver
    ├── scripts/
    │   ├── kill-worker.sh
    │   ├── reset-receiver.sh
    │   ├── restart-worker.sh
    │   └── stop-demo-and-reset.sh
    ├── exercise/
    │   └── 01-06-*/src/*.ts    # Starter code with TODO markers
    └── solution/
        └── 01-06-*/src/*.ts    # Completed code
```

## Modules

| # | Name | Goal | Fail-then-fix |
|---|------|------|---------------|
| 01 | **Durable job queue** | Write `deliverWebhook`, submit via `client.activity.execute`, inspect in UI. | Introduction, no fail-then-fix. |
| 02 | **Idempotency and crash safety** | Make retries safe with `Idempotency-Key` header. | Reproduce 3 POSTs for 1 event; add one header; receiver dedupes. |
| 03 | **Dedup via ID reuse** | Use `ActivityIdConflictPolicy.USE_EXISTING` to reject duplicate starts. | Second `start()` with same id errors; add policy; both succeed. |
| 04 | **Concurrency and rate limits** | Cap Worker throughput with `maxActivitiesPerSecond`. | Unbounded fan-out floods receiver with 429s; add rate cap; 429s stop. |
| 05 | **Heartbeats and checkpointing** | Read `heartbeatDetails` on retry to resume from checkpoint. | Kill Worker mid-batch; retry restarts from 0; add checkpoint read; resumes cleanly. |
| 06 | **Same code runs anywhere** | Same `deliverWebhook` submitted standalone and as a Workflow step. | Capstone; no fail-then-fix. |

## TypeScript SDK API notes

- `client.activity.execute(fn, opts)` - submit + wait
- `client.activity.start(fn, opts)` - submit, returns `ActivityHandle`
- `ActivityIdConflictPolicy` from `@temporalio/client`
- `Context.current().heartbeat(value)` and `Context.current().info.heartbeatDetails`
- `ApplicationFailure.create({message})` from `@temporalio/activity`
- `proxyActivities<typeof activities>(opts)` for Workflow step (Module 06)
- Worker option `maxActivitiesPerSecond` (Module 04)

## Module resolution in sandbox

- `node_modules` installed at `/opt/workshop/node_modules` in the Docker image.
- `track_scripts/setup-workshop` creates symlinks:
  - `/root/workshop/node_modules` → `/opt/workshop/node_modules`
  - `/root/workshop/tsconfig.json` → `/opt/workshop/tsconfig.json`
- Node.js walks up from any exercise directory and finds `node_modules` at `/root/workshop/`.
- Attendees run: `ts-node src/worker.ts` and `ts-node src/sendStandalone.ts evt_001`.

## First push checklist

1. Create a GitHub repo for the track definition (this repo).
2. Build and push the sandbox image: `docker build -t ghcr.io/temporalio/edu-standalone-activities-ts-sandbox:latest ./sandbox && docker push`.
3. Create the Instruqt track slug: `instruqt track create standalone-activities-typescript --title "Build a Job Queue with Standalone Activities (TypeScript)"`.
4. Push the track: `cd instruqt && instruqt track push --force`.
5. Pull the server-assigned ids: `cd .. && instruqt track pull`.
6. Commit the populated `track.yml` (with ids filled in).

## Open questions

1. Verify `ActivityIdConflictPolicy` export path in `@temporalio/client` v1.11+ (may be `@temporalio/common`).
2. Verify `client.activity.execute` / `client.activity.start` API shape matches code in Module 01/03.
3. Task Queue Priority and Fairness API availability in TypeScript SDK (Module 04 currently only references it conceptually).
4. Pin `@temporalio/*` to a specific version in `package.json` once the standalone activities API is stable in a GA release.
