# Standalone Activities as a Durable Job Queue (Java)

The Java edition of the "Build a Job Queue with Standalone Activities" tutorial.
It mirrors the [Python reference](../python) and the [TypeScript edition](../typescript)
module-for-module, translated to idiomatic Java with the Temporal Java SDK.

Learners build a durable webhook-delivery service across six modules: submit a
durable job, make retries safe with idempotency, dedupe duplicate submissions,
rate-cap a fan-out, checkpoint a long-running batch with heartbeats, and reuse
the same Activity from a Workflow.

## Run the code locally (optional)

Each module under `course-repo/exercise/<NN>` and `course-repo/solution/<NN>` is a
self-contained Gradle project. With a Temporal dev server and the webhook receiver
running:

```bash
# Temporal dev server + Web UI on http://localhost:8233
temporal server start-dev --ui-port 8233

# Shared webhook receiver on :9000
python3 ../shared/webhook-receiver/webhook_receiver.py

# A module's Worker (from the module dir)
cd course-repo/solution/01-durable-job-queue
gradle -q execute -PmainClass=webhook.Worker

# Submit a job (another terminal, same module dir)
gradle -q execute -PmainClass=webhook.SendStandalone -PappArgs=evt_001
```

The Instruqt lab is the supported path; local runs are for the curious.

## Instruqt track

### Directory structure

```
java/
├── instruqt/                 # Track definition
│   ├── track.yml             # Track config (no challenges: list; auto-discovered)
│   ├── config.yml            # Container image reference (:java-latest)
│   ├── track_scripts/        # setup-workshop, cleanup-workshop
│   └── NN-<slug>/            # One per challenge
│       ├── assignment.md     # Frontmatter + markdown body
│       ├── setup-workshop    # Per-challenge reset
│       ├── check-workshop    # Instant pass (exit 0)
│       └── solve-workshop    # Copies solution -> exercise (for free navigation)
├── sandbox/Dockerfile        # Built + pushed to GHCR
├── course-repo/              # Baked into the image at /opt/workshop
│   ├── exercise/NN/          # Starter code with TODO markers (per-module Gradle project)
│   ├── solution/NN/          # Completed code
│   ├── scripts/              # kill-worker, reset-receiver, restart-worker, stop-demo-and-reset
│   └── demos/                # Interactive HTML diagrams served on :9001
└── scripts/verify-content.sh # Content guardrails
```

The SDK-agnostic webhook receiver lives at `../shared/webhook-receiver/webhook_receiver.py`
and is shared across all language editions.

### What the sandbox image bakes in

- Eclipse Temurin 21 JDK
- Gradle 8.10.2
- Temporal CLI 1.7.2 (bundles a dev server with Standalone Activities enabled, and
  the `temporal activity` subcommands + Standalone Activities UI tab)
- python3 (runs the shared webhook receiver on :9000 and a static demo server on :9001)
- All 6 course modules, pre-built at image-build time so the first `gradle execute`
  boots fast

### Tab inventory (every module)

| Index | Tab | Type |
|---|---|---|
| tab-0 | Temporal UI | service (:8233), landing tab |
| tab-1 | Exercise | code editor |
| tab-2 | Solution | code editor |
| tab-3 | Terminal | terminal |
| tab-4 | Worker | terminal |
| tab-5 | Webhook receiver | service (:9000) |
| tab-6 | Interactive Diagram | service (:9001), Module 05 only |

### Instruqt CLI workflow

```bash
cd java/instruqt
instruqt track validate
instruqt track push --force   # first push; writes id + tab ids back
instruqt track pull           # populate server-assigned ids
```

### First-time track creation

```bash
# Build + push the sandbox image (context is the repo root)
docker buildx build --platform linux/amd64 -f java/sandbox/Dockerfile \
  -t ghcr.io/temporalio/edu-standalone-activities-sandbox:java-latest --push .

# Register the slug, then push the track
instruqt track create standalone-activities-java \
  --title "Build a Job Queue with Standalone Activities (Java)"
cd java/instruqt && instruqt track push --force
instruqt track pull
```

CI (`.github/workflows/build-sandbox-java.yml` + `push-track-java.yml`) rebuilds the
image and pushes the track automatically on merges to `main` that touch Java paths.

## Known issues

- **Standalone Activities are Public Preview in the Java SDK 1.36.1.** The
  `ActivityClient` / `StartActivityOptions` API may change before GA. The track pins
  `io.temporal:temporal-sdk:1.36.1`; bump it deliberately and re-verify against the
  [Java Standalone Activities docs](https://docs.temporal.io/develop/java/activities/standalone-activities).
- **Module 02's idempotency demo iframe is branch-pinned to `main`.** The
  `assignment.md` embeds `https://raw.githack.com/temporalio/edu-standalone-activities/main/docs/java-idempotency-demo/index.html`.
  It 404s until this branch merges to `main`. The Module 05 Interactive Diagram is
  served in-sandbox (:9001) and has no such dependency.
- **Tab order deviates from the TypeScript edition.** This track lands on the Temporal
  UI tab (canonical order, per AGENTS.md). The TypeScript track predates that
  convention. See `PRD.md`.
- **`gradle -q execute` startup adds a second or two** to each command versus a
  compiled binary. Timings in the assignments account for this ("a few seconds").
