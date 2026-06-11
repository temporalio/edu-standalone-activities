# Standalone Activities as a Durable Job Queue (Python)

Source for the Instruqt-delivered Python tutorial covering Temporal Standalone Activities. See [`PRD.md`](./PRD.md) for the course design and outline.

## Layout

```
python/
├── PRD.md                 # Course design / outline
├── README.md              # This file
├── instruqt/              # Instruqt track config (track.yml + per-challenge folders)
├── sandbox/               # Dockerfile for the Instruqt sandbox base image
└── course-repo/           # Contents pre-cloned to /opt/workshop inside the sandbox
    ├── pyproject.toml
    ├── server/            # Stdlib webhook receiver
    ├── scripts/           # Chaos helpers (kill-worker, reset-receiver, ...)
    ├── exercise/<NN>/     # Starter code per module (has TODO markers)
    └── solution/<NN>/     # Completed code per module
```

## Status

All six modules in place:

1. **`01-durable-job-queue`** - the durable job queue introduction.
2. **`02-idempotency-and-crash-safety`** - idempotency under Temporal's own retries.
3. **`03-dedup-via-id-reuse`** - server-side dedup via `id_conflict_policy`.
4. **`04-concurrency-and-rate-limits`** - rate cap, Priority and fairness.
5. **`05-heartbeats-and-checkpointing`** - long-running Activity, `activity.heartbeat()`, resumption from `heartbeat_details`.
6. **`06-same-code-runs-anywhere`** - the same Activity submitted directly and as a Workflow step.

## Local development

```bash
# From inside the sandbox image (or a local equivalent):
cd /opt/workshop
uv sync
uv run python server/webhook_receiver.py &      # Start webhook receiver on :9000
temporal server start-dev --ui-port 8233 & # Start Temporal dev server

cd exercise/01-durable-job-queue
uv run python -m webhooks.worker &         # Start the worker
uv run python -m webhooks.send_standalone evt_001
uv run python -m webhooks.send_via_workflow evt_002
curl http://localhost:9000/_received
```

## Implementation decisions (verified locally)

- HTTP client: `httpx` (sync mode).
- `temporalio>=1.27` - verified that `Client.execute_activity` and `Client.start_activity` are exposed in 1.27.2 (built and smoke-tested locally).
- Temporal CLI: `v1.6.2-standalone-activity` - verified as a real published release; ships `temporal activity describe/list/count` (all marked Experimental).

## Open questions

- `start_activity` ID conflict policy is used in Module 03 (`id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING`); verify the pinned `temporalio` version in `pyproject.toml` still exposes it.
- Task Queue Priority and Fairness is exercised in Module 04 (`Priority(priority_key, fairness_key, fairness_weight)`); verify against the pre-release Temporal CLI pinned in `sandbox/Dockerfile`.
