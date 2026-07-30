# Design: Go SDK Instruqt track — "Build a Job Queue with Standalone Activities (Go)"

**Date:** 2026-07-07
**Author:** Nikolay Advolodkin (via Claude Code)
**Branch:** `add-go-track`

## Context

The repo `edu-standalone-activities` teaches Temporal **Standalone Activities**
as a durable job queue through a six-module webhook-delivery narrative. A
complete **Python** track lives on `main` (`python/`) and a complete
**TypeScript** track lives on the `add-typescript-track` branch
(`typescript/`). This project adds a **Go** track (`go/`) that mirrors the
TypeScript track's structure and pedagogy, adapted to Go SDK idioms.

The user chose: mirror the TypeScript track but adapt code/idioms for Go;
lay each module out with a subdir-per-command structure; and take the work all
the way through the full pipeline (build + push the sandbox image, push the
Instruqt track).

## Verified Temporal Go SDK facts

All verified against the Temporal Docs MCP (`temporal.mcp.kapa.ai`) and
`pkg.go.dev` on 2026-07-07. Standalone Activities are **Public Preview**;
`ClientStartActivityOptions` was added in SDK **v1.40.0** (targeting latest,
v1.45). CLI **v1.7.2** and Server **v1.31.0+** are required for the
`temporal activity` subcommands and the Standalone Activities UI tab.

| Concept | Go API | Source |
|---|---|---|
| Submit standalone job | `handle, err := c.ExecuteActivity(ctx, client.StartActivityOptions{...}, DeliverWebhook, req)`; `handle.Get(ctx, &res)` blocks for the result | docs.temporal.io/develop/go/activities/standalone-activities#execute-activity |
| Options | `client.StartActivityOptions{ID, TaskQueue, StartToCloseTimeout}` — requires `ID`, `TaskQueue`, and one of `StartToCloseTimeout`/`ScheduleToCloseTimeout` | pkg.go.dev `ClientStartActivityOptions` |
| Handle | `handle.GetID()`, `handle.GetRunID()`, `handle.Get(ctx, &out)` | docs (samples-go starter) |
| Dedup (M03) | `ActivityIDConflictPolicy: enums.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING` (`go.temporal.io/api/enums/v1`); default returns `ActivityExecutionAlreadyStarted` | pkg.go.dev + docs |
| Rate cap (M04) | `worker.Options{WorkerActivitiesPerSecond: 2, MaxConcurrentActivityExecutionSize: 10}` (per-worker cap) | pkg.go.dev `WorkerOptions` |
| Heartbeat/resume (M05) | `activity.RecordHeartbeat(ctx, n)`; on retry `activity.HasHeartbeatDetails(ctx)` + `activity.GetHeartbeatDetails(ctx, &n)`; `activity.GetInfo(ctx).Attempt` | docs.temporal.io/develop/go/activities/timeouts#activity-heartbeats |
| Activity-in-Workflow (M06) | `workflow.ExecuteActivity(ctx, DeliverWebhook, req).Get(ctx, &res)` under `workflow.WithActivityOptions` | pkg.go.dev `workflow.ExecuteActivity` |
| Worker registration | Identical for standalone and workflow-driven: `w.RegisterActivity(DeliverWebhook)` (+ `w.RegisterWorkflow(...)` in M06) | docs standalone-activities#run-worker |
| Logging | `activity.GetLogger(ctx).Info(...)` | Go SDK reference |

**Key Go-specific difference:** Go exposes only `ExecuteActivity` (returns a
handle immediately, non-blocking); there is no separate `start`/`execute`
split like TypeScript. Blocking is done by calling `handle.Get()`. This suits
the chaos demos (M02/M05), which per AGENTS.md need a non-blocking starter so
the learner can crash the Worker in the same session.

## Architecture

New top-level `go/` tree, structurally parallel to `typescript/`:

```
go/
├── PRD.md                      # Go course outline (adapted from TS PRD)
├── README.md                   # short "run locally" pointer
├── scripts/
│   └── verify-content.sh       # content guardrails, ported for Go (Module-06 grep adapted to Go imports)
├── diagrams/                   # (optional) any Go-specific diagrams; reuse shared demos otherwise
├── instruqt/
│   ├── track.yml               # slug: standalone-activities-go
│   ├── config.yml              # image: ...sandbox:go-latest, container "workshop"
│   ├── track_scripts/{setup-workshop,cleanup-workshop}
│   └── NN-<slug>/              # 6 challenges: assignment.md + setup/check/solve-workshop
├── sandbox/
│   ├── Dockerfile              # golang base + Temporal CLI v1.7.2, prebuilt binaries
│   └── .dockerignore
└── course-repo/
    ├── go.mod / go.sum         # one module at course-repo root
    ├── server/                 # Go ports of the receiver + demo server
    │   ├── webhookreceiver/main.go
    │   └── demoserver/main.go
    ├── demos/                  # static HTML demos reused from TS/Python (heartbeat, conflict-policy)
    ├── scripts/{kill-worker.sh,restart-worker.sh,reset-receiver.sh,stop-demo-and-reset.sh}
    ├── exercise/NN-<slug>/     # starter (compiling TODO stubs)
    └── solution/NN-<slug>/     # completed
```

### Per-module Go layout (subdir-per-command)

Each `exercise|solution/NN-<slug>/` directory contains:

```
NN-<slug>/
├── webhook/              # shared library package the learner edits
│   ├── shared.go         # TaskQueue, WebhookReceiverURL, WebhookDelivery struct
│   ├── activity.go       # DeliverWebhook (the module's teaching artifact)
│   └── workflow.go       # (M01 + M06 only) WebhookWorkflow calling DeliverWebhook
├── worker/main.go        # package main — registers + runs the Worker
├── sendstandalone/main.go# package main — ExecuteActivity + handle.Get
└── ...                   # per-module extra commands (sendviaworkflow, senddouble, sendbulk, sendbatch)
```

Learners run `go run ./worker` and `go run ./sendstandalone evt_001` from the
module directory. One `go.mod` at `course-repo/` root; `go run ./<cmd>` resolves
the enclosing module by walking up to it. Import paths per module are unique, so
identical type names across modules never collide.

### Per-module command inventory (mirrors TS)

| # | Slug | webhook/ files | commands | teaches |
|---|---|---|---|---|
| 01 | durable-job-queue | shared, activity, workflow | worker, sendstandalone, sendviaworkflow | `ExecuteActivity`, UI Standalone Activities tab |
| 02 | idempotency-and-crash-safety | shared, activity | worker, sendstandalone | `Idempotency-Key` header; crash-safe retries |
| 03 | dedup-via-id-reuse | shared, activity | worker, sendstandalone, senddouble | `ActivityIDConflictPolicy` USE_EXISTING |
| 04 | concurrency-and-rate-limits | shared, activity | worker, sendbulk, sendbulkdemo, sendstandalone | `WorkerActivitiesPerSecond` |
| 05 | heartbeats-and-checkpointing | shared, activity | worker, sendbatch | heartbeat + `GetHeartbeatDetails` resume |
| 06 | same-code-runs-anywhere | shared, activity, workflow | worker, sendstandalone, sendviaworkflow | same `DeliverWebhook`, two callers |

## Servers (ported to Go)

The webhook receiver and demo server are infrastructure, not teaching content;
they are rewritten in Go (`net/http`) so the image carries no Node runtime.

- **`server/webhookreceiver/main.go`** — port of `webhookReceiver.ts`. Same
  endpoints and JSON shape (snake_case keys: `received_count`, `processed_count`,
  `deduped_count`, `throttled_count`, `rate_limit`, `count`, `deliveries`),
  same routes (`POST /hooks`, `POST /_reset`, `POST /_rate_limit?limit=N`,
  `GET /_received`, `GET /` HTML dashboard auto-refreshing every 2s), same
  idempotency-key dedup and sliding-window rate limiting on port 9000.
- **`server/demoserver/main.go`** — port of `demoServer.ts`; static file server
  for `demos/` on port 9001 with permissive CORS/frame headers for iframing.

Static HTML demos (heartbeat, conflict-policy) are reused verbatim from the
existing tracks.

## Instruqt track

Mirror `typescript/instruqt` exactly except for Go specifics:

- **track.yml**: `slug: standalone-activities-go`, title
  "Build a Job Queue with Standalone Activities (Go)"; same `lab_config`
  (modern-dark, AssignmentLeft, sidebar 33, `skipping_enabled: true`,
  loadingMessages with Go-flavored SDK lines). `id`/`checksum` filled by first push.
- **config.yml**: container `workshop`, image
  `ghcr.io/temporalio/edu-standalone-activities-sandbox:go-latest`, memory 4096.
- **track_scripts/setup-workshop**: boot `temporal server start-dev`
  (single partition), the Go receiver + demo-server **binaries** (built into the
  image, not `ts-node`), wait for ports 7233/8233/9000/9001, seed all 6 modules'
  `exercise/` and `solution/` under `/root/workshop/exercises/`, copy `scripts/`.
  No `node_modules` symlink; instead rely on the module cache pre-warmed at
  image build (`go build ./...`).
- **Per-challenge assignment.md**: same frontmatter shape and **same canonical
  tab order** (Temporal UI first as landing tab; then Exercise, Solution,
  Terminal, Worker, Webhook receiver, plus per-module demo where TS has one).
  Tab paths point at the Go module dirs. Bodies rewritten for Go: `go run ./worker`,
  Go code snippets, Go file paths (`webhook/activity.go`, `sendstandalone/main.go`).
  All tab references use clickable `[button ...](tab-N)` with N matching the
  label's position. Keep the feedback-form footer.
- **setup/check/solve-workshop** per challenge. With `skipping_enabled: true`,
  every challenge needs a `solve-workshop` (cheap `cp -rf solution → exercise`)
  and check can be an instant `exit 0` (exploratory track, matching the
  existing tracks' free-navigation pattern).

Script suffix (`-workshop`) matches the container name `workshop`.

## Sandbox image

`go/sandbox/Dockerfile`:

- Base `golang:1.24-bookworm` (has the Go toolchain for `go run` in-session).
- System deps: ca-certificates, curl, git, jq, procps.
- Temporal CLI **v1.7.2** (linux/amd64) — same as TS.
- `COPY course-repo/ /opt/workshop`, then `cd /opt/workshop && go build ./...`
  to (a) pre-warm the module cache so first `go run` is fast and (b) validate
  that every solution + compiling-stub exercise builds at image-build time.
- Build the receiver + demo-server binaries into `/usr/local/bin`.
- `chmod +x scripts/*.sh`; `EXPOSE 7233 8233 9000 9001`.
- Built and pushed for **linux/amd64** via `docker buildx`.

## CI

Mirror the TS workflows for Go:

- `.github/workflows/build-sandbox-go.yml` — buildx build+push `:go-latest` on
  changes under `go/course-repo/**` or `go/sandbox/**`.
- `.github/workflows/push-track-go.yml` — `instruqt track push --force` on merge
  to `main` touching `go/instruqt/**` / `go/course-repo/**` / `go/sandbox/**`,
  gated on the image build.

## Content guardrails

Port `python/scripts/verify-content.sh` to `go/scripts/verify-content.sh`,
keeping all checks (banned SAA-vs-Workflow framing, no competitor names,
slug/dir match, no em dashes, Standalone-Activities UI over-claim guard) and
adapting **check 4** (Module-06 "same Activity, two callers"): both callers must
import `DeliverWebhook` from the same `webhook` package instead of the Python
`.activities` module.

## Deliverable / verification (full pipeline)

1. `go build ./...` — validated inside the amd64 Docker build (local Go absent).
2. `go/scripts/verify-content.sh` — content guardrails, exit 0.
3. `cd go/instruqt && instruqt track validate` — schema/script check, exit 0.
4. `docker buildx build --platform linux/amd64 --push` the sandbox image to
   `ghcr.io/temporalio/edu-standalone-activities-sandbox:go-latest`.
   **Requires** `gh auth refresh -h github.com -s write:packages,read:packages`
   (current token lacks `write:packages`) and GHCR package set Public.
5. `instruqt track create` (first time) + `instruqt track push --force`, then
   `instruqt track pull` to capture server-assigned `id`s; commit them.
6. Local end-to-end walk-through per AGENTS.md "Testing your track locally":
   run each module's worker + starter from both exercise/ and solution/ inside
   the container.

## Preflight gaps to resolve during execution

- `gh` token needs `write:packages` (user runs the refresh via `!`).
- Instruqt auth (`instruqt auth login`) confirmed before push.
- Go not installed locally — compilation gate is the Docker build; optionally
  install Go to lint/vet locally.

## Non-goals

- No changes to the existing Python or TypeScript tracks.
- No new teaching modules beyond the six mirrored ones.
- No production Temporal Cloud wiring (dev server only, like the other tracks).
