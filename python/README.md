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
    ├── server/            # Stdlib echo server (the webhook receiver)
    ├── scripts/           # Chaos helpers (kill-worker, reset-echo, ...)
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
uv run python server/echo_server.py &      # Start echo server on :9000
temporal server start-dev --ui-port 8233 & # Start Temporal dev server

cd exercise/01-skip-the-workflow
uv run python -m webhooks.worker &         # Start the worker
uv run python -m webhooks.send_standalone evt_001
uv run python -m webhooks.send_via_workflow evt_002
curl http://localhost:9000/_received
```

## Open implementation questions

Tracked in [`PRD.md`](./PRD.md). Pinned defaults for now:

- HTTP client: `httpx` (sync mode).
- `start_activity` ID conflict policy: TODO comment in Module 04 until SDK behavior is verified.
- Task Queue Priority and Fairness: depth depends on SDK exposure; Module 03 will adapt at implementation time.
- `temporalio` version pin: `>=1.18` placeholder. The exact SDK build that supports `client.execute_activity` directly (i.e. the prerelease standalone-activities branch) needs to be confirmed and pinned before publication.
