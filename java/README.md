# Build a Job Queue with Standalone Activities (Java)

The Java edition of the Standalone Activities tutorial. It teaches Temporal's durable job
queue by building a webhook-delivery service across six modules: durable submit, idempotency
and crash safety, dedup via ID reuse, concurrency and rate limits, heartbeats and
checkpointing, and the Activity-in-Workflow upgrade path.

## Layout

```
java/
├── course-repo/
│   ├── exercise/<NN-slug>/   # starter code (TODO markers), one self-contained Gradle project each
│   ├── solution/<NN-slug>/   # completed code
│   ├── demos/                # interactive HTML served on :9001
│   └── scripts/              # kill-worker, reset-receiver, restart-worker, stop-demo-and-reset
├── instruqt/                 # Instruqt track (track.yml, config.yml, per-challenge dirs)
├── sandbox/Dockerfile        # sandbox image (Temurin 21 + Gradle + Temporal CLI + python3)
└── scripts/verify-content.sh # content guardrails
```

The webhook receiver is **not** vendored here. It is the shared, SDK-agnostic receiver at
`../shared/webhook-receiver/webhook_receiver.py` (Python stdlib, reused across language
tracks). The sandbox also serves the demo HTML with `python -m http.server` on port 9001.

## SDK

Temporal Java SDK **1.36.1** (Standalone Activities, Public Preview): `ActivityClient.execute` /
`ActivityClient.start` return an `ActivityHandle`. Requires Temporal Server >= 1.31 (bundled in
Temporal CLI >= 1.7).

## Run a module locally

Each module is an independent Gradle project. Run these from the `java/` directory:

```bash
# Terminal 1: Temporal dev server (Standalone Activities enabled by default)
temporal server start-dev --ui-port 8233

# Terminal 2: the shared webhook receiver
python3 ../shared/webhook-receiver/webhook_receiver.py

# Terminal 3: the Worker
cd course-repo/exercise/01-durable-job-queue
gradle -q execute -PmainClass=webhook.Worker

# Terminal 4: submit a job
gradle -q execute -PmainClass=webhook.SendStandalone -PappArgs=evt_001
```

## Before opening a PR

```bash
bash java/scripts/verify-content.sh
cd java/instruqt && instruqt track validate
```

The Java sandbox image builds from the **repository root** context (so it can reach
`shared/`):

```bash
docker buildx build --platform linux/amd64 \
  -f java/sandbox/Dockerfile \
  -t ghcr.io/temporalio/edu-standalone-activities-sandbox:java-latest --push .
```
