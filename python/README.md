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

Currently shipping Module 01 + sandbox skeleton. Modules 02–06 follow in subsequent PRs.

## Local development

```bash
# From inside the sandbox image (or a local equivalent):
cd /opt/workshop
uv sync
uv run python server/webhook_receiver.py &      # Start webhook receiver on :9000
temporal server start-dev --ui-port 8233 & # Start Temporal dev server

cd exercise/01-skip-the-workflow
uv run python -m webhooks.worker &         # Start the worker
uv run python -m webhooks.send_standalone evt_001
uv run python -m webhooks.send_via_workflow evt_002
curl http://localhost:9000/_received
```

## Implementation decisions (verified locally)

- HTTP client: `httpx` (sync mode).
- `temporalio>=1.27` — verified that `Client.execute_activity` and `Client.start_activity` are exposed in 1.27.2 (built and smoke-tested locally).
- Temporal CLI: `v1.6.2-standalone-activity` — verified as a real published release; ships `temporal activity describe/list/count` (all marked Experimental).

## Open questions for future modules

- `start_activity` ID conflict policy: needed for Module 04; verify the exact kwarg name in the Python SDK at implementation time.
- Task Queue Priority and Fairness: depth in Module 03 depends on SDK exposure; verify when authoring Module 03.
