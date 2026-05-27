# PRD: "Bulk Email at Scale" — Python Standalone Activities Instruqt Track

## Context

Temporal needs a hands-on text-and-sandbox tutorial that teaches Python **standalone activities** (the workflow-less execution path documented at https://docs.temporal.io/develop/python/activities/standalone-activities) via a realistic use case. The goal is a polished tutorial in the style of https://learn.temporal.io/tutorials/nexus/nexus-sync-tutorial-java/, but delivered on Instruqt so learners install nothing locally and can verify Temporal's durability guarantees with their own eyes.

**Why this specific shape:**
- Standalone activities are best motivated by jobs that are discrete, externally flaky, and don't need orchestration. SMTP send is the textbook fit: retries matter, no workflow needed, each call is independent.
- Email lets the tutorial avoid third-party accounts and API keys entirely — MailPit runs in the sandbox and provides a visible inbox.
- Instruqt removes setup friction (the #1 reason tutorials get abandoned) and gives us three observable surfaces side-by-side: editor + Temporal Web UI + MailPit inbox. Each makes a different invariant visible.
- The "kill the worker mid-batch / watch it resume" demo is the strongest possible payoff for a standalone-activities tutorial. The bulk-send shape gives that demo enough wall-clock and enough work-in-flight to be visceral.

**Intended outcome:** a published Instruqt track (~80 min, 8 challenges including prologue and recap) that a Python developer with no Temporal experience can complete end-to-end and walk away understanding `@activity.defn`, `client.execute_activity`, RetryPolicy, heartbeats, cancellation, and task queue isolation — proven by two destructive moments where Temporal's durability becomes literally visible.

## Reference repos to mirror

- **Track skeleton (authoritative):** `github.com/temporalio/workshop-nexus-intro-instruqt` — copy its layout convention (`track.yml`, per-challenge folders with `setup-workshop` / `solve-workshop` / `check-workshop` / `assignment.md` / `challenge.yml`), its sandbox-image pattern, and its assignment-writing voice. Match the AssignmentRight layout, dark theme, sidebar 40%.
- **Canonical Python code patterns:** the `hello_standalone_activity` directory of `github.com/temporalio/samples-python` for `@activity.defn`, worker registration, `client.execute_activity`, and `client.start_activity` shapes. Lift these verbatim with the function bodies swapped for SMTP sends.
- **Retry / heartbeat references:** `hello/hello_activity_retry.py` and `hello/hello_activity_heartbeat.py` in the same samples repo.

## Track shape

- **Title:** "Bulk Email at Scale: Python Standalone Activities with Temporal"
- **Duration:** ~80 min total, 8 challenges (00 prologue through 07 recap). Every challenge follows the same structure: **~5 min teaching, then a hands-on activity** (target ~5–7 min). This is a hard rule — no challenge is reading-only or activity-only.
- **Layout:** Terminal + Editor + Browser tabs (browser shows MailPit at `localhost:8025` and Temporal Web UI at `localhost:8233`).
- **Sandbox base image:** custom Docker image based on `python:3.13-slim`, mirroring the Nexus workshop's pattern. Pre-bakes: Python 3.13, `uv`, **prerelease Temporal CLI `v1.6.2-standalone-activity`** (so `temporal activity list/count` work in checks), MailPit binary, `tmux`, `git`, `procps`. Pre-cloned course repo at `/opt/workshop/` with `exercise/`, `solution/`, `data/contacts.csv`, `scripts/` (the chaos helpers), and a pre-warmed `uv` venv.
- **Track-level setup:** start `temporal server start-dev --ui-port 8233` and `mailpit --smtp 0.0.0.0:1025 --listen 0.0.0.0:8025` in the background, wait for both ports, then `cp /opt/workshop/exercise/00-prologue/. ~/workshop/`.

### Module structure (applies to every challenge)

Each `assignment.md` is split into exactly two sections with these headings:

1. **`## Concept (5 min)`** — ~600–800 words of focused teaching: one core idea, one diagram or code-shape illustration, one analogy. No commands to run. Reader should be able to skim-or-read in 5 min and form the mental model needed for the activity.
2. **`## Hands-on (5–7 min)`** — numbered steps the learner executes: edit specific files (with TODO markers), run specific commands, observe specific outputs. Each step ends with an "Expected output" block so learners can self-verify before pressing Check. The Check button validates the same outcome the step describes.

The prologue's hands-on is a 3-step panel tour (open MailPit, open Temporal UI, run `temporal operator cluster health`). The recap's hands-on is running `temporal activity list` to inspect everything they created plus answering a 2-question knowledge check.

## Challenges

All challenges follow the `## Concept (5 min)` + `## Hands-on (5–7 min)` structure described above. Length column is total per-challenge time.

| # | Slug | Concept taught (5 min) | Hands-on (5–7 min) | Length |
|---|------|------------------------|---------------------|--------|
| 00 | `prologue` | The scenario (flaky SMTP, fan-out send), the four panes, what a standalone activity is vs. a workflow-bound one | Tour the panes; open MailPit and Temporal UI; run `temporal operator cluster health` | ~10 min |
| 01 | `define-and-execute` | `@activity.defn` decorator, worker registration, `client.execute_activity` shape | Write `send_email`, register in worker, fire one against MailPit, verify in inbox + Web UI | ~12 min |
| 02 | `bulk-fanout` | `execute_activity` vs. `start_activity`; concurrency under `ThreadPoolExecutor(N)` | Fan out 50 emails from `contacts.csv` via `asyncio.gather`; observe concurrency cap | ~12 min |
| 03 | `durability-kill-worker` | Where activity state lives (server, not worker); default RetryPolicy; "worker is cattle" | **DURABILITY MOMENT #1.** Fire 200; `pkill -f worker.py`; restart; watch MailPit resume and Temporal UI show `attempt > 1` | ~12 min |
| 04 | `retries-and-mailpit-down` | `RetryPolicy` fields; `start_to_close_timeout` vs `schedule_to_close_timeout`; server-driven retry loop | **DURABILITY MOMENT #2.** Tune RetryPolicy; `pkill -STOP mailpit`; observe pending retries; `pkill -CONT mailpit`; observe completion | ~13 min |
| 05 | `heartbeats-and-cancel` | When you need heartbeats; `heartbeat_timeout`; cancellation propagation via heartbeats | Add `activity.heartbeat()` to a 100-recipient batch activity; cancel mid-flight from client; inspect partial progress | ~12 min |
| 06 | `task-queue-isolation` | Task queue as routing key; when to split workloads; not a permission boundary | Run two workers on `email-marketing` and `email-transactional`; kill marketing worker; verify transactional keeps going | ~12 min |
| 07 | `recap-and-next` | When standalone vs. workflow; pointer to what's beyond (signals, schedules, workflows) | Run `temporal activity list` to inspect all activities created; answer 2-question knowledge check | ~7 min |

### Per-challenge file pattern (all challenges)

Each challenge folder contains:
- `assignment.md` — strictly two sections: `## Concept (5 min)` (~600–800 words, no commands) and `## Hands-on (5–7 min)` (numbered steps with "Expected output" blocks). Total length per file is meaningfully shorter than the Nexus workshop's 1.2K–2K-word assignments — the constraint is the 5-minute teaching cap, not the reference track's word count.
- `setup-workshop` — copies fresh files for this challenge from `/opt/workshop/exercise/<NN-slug>/` into `~/workshop/` without clobbering learner edits; clears MailPit inbox via `curl -X DELETE http://localhost:8025/api/v1/messages`.
- `solve-workshop` — overlays `/opt/workshop/solution/<NN-slug>/` onto `~/workshop/` for the "Show solution" button.
- `check-workshop` — kills any leftover worker, starts the learner's worker, runs the starter script, polls MailPit's REST API and `temporal activity list` until assertions pass (60s timeout); exits non-zero with a diff on failure.
- `challenge.yml` — title, type, timelimit, tabs config matching the reference track's pattern.

### Code that ships in `/opt/workshop/`

By end of track, learner has:
- `src/bulkmail/activities.py` — `send_email`, `send_email_batch` (with heartbeats)
- `src/bulkmail/worker.py`, `worker_transactional.py`
- `src/bulkmail/send_one.py`, `send_bulk.py`, `cancel_demo.py`
- `scripts/kill-worker.sh`, `restart-worker.sh`, `stop-mailpit.sh` (`pkill -STOP mailpit`), `start-mailpit.sh` (`pkill -CONT mailpit`)
- `data/contacts.csv` — 200 fake recipients used by `send_bulk.py`
- `pyproject.toml` pinning `temporalio>=1.18`

## Implementation decisions (locked)

- **Prerelease CLI bundled** so checks and learner-facing inspection can use `temporal activity list/count`.
- **MailPit runs as a binary**; outage simulated via `SIGSTOP`/`SIGCONT` (TCP accepts pause without losing inbox or PID).
- **Sync `smtplib`** + `activity_executor=ThreadPoolExecutor(5)` — more relatable to most Python learners and motivates the executor knob.
- **No workflows anywhere** in the track. Workflows are explicitly "what comes next" in the recap.
- **No idempotency challenge** in v1 — mentioned in the recap as a real-world consideration but kept out of scope to hold the track at 8 challenges.

## Out of scope

Workflows, signals/queries/updates, async-completion activities, local activities, worker versioning, data converters, Temporal Cloud connection setup, schedules. Each is named in `07-recap-and-next` with a doc link.

## Critical authoring work, in order

1. **Sandbox image.** Author the Dockerfile mirroring the Nexus workshop's pattern; pin the prerelease Temporal CLI tag; bake MailPit binary; pre-clone the course repo and `uv sync` so first-challenge boot is warm.
2. **Course repo.** Build `exercise/` (starter with `# TODO` markers) and `solution/` (completed code) trees. Activities and clients in `solution/` come from `samples-python/hello_standalone_activity` with SMTP-flavored function bodies. Use `data/contacts.csv` with 200 rows so challenge 03's kill-worker demo has enough wall-clock.
3. **track.yml + per-challenge configs.** Copy structure from `workshop-nexus-intro-instruqt`; update title, owner, tags, ordering, base image reference.
4. **Per-challenge scripts.** Write `setup-workshop` / `solve-workshop` / `check-workshop` for each of the 8 challenges. Check scripts poll MailPit's `GET /api/v1/messages` for expected `To:`/`Subject:` matches and shell out to `temporal activity list --query ...` for attempt-count assertions.
5. **Assignments.** Draft `assignment.md` per challenge following the `Concept (5 min)` + `Hands-on (5–7 min)` template, with one verifiable expected-output block per step.
6. **Internal dry-run.** Publish to a private Instruqt org, run the track end-to-end timing each challenge, tune setup scripts and assignment pacing until total wall-clock is ~80 min.

## Verification

End-to-end the track is "done" when:
- A fresh Temporal teammate runs the track on Instruqt with no prior setup and finishes in 60–90 min.
- Every `check-workshop` passes against the learner's solution path AND against `solve-workshop` overlay.
- Challenge 03's destructive moment: after `pkill -f worker.py`, the Temporal Web UI shows pending activities with attempts climbing; after restart, MailPit reaches exactly 200 unique messages and `temporal activity list --query "ExecutionStatus='Completed'"` returns 200.
- Challenge 04's destructive moment: after `pkill -STOP mailpit`, worker logs flood with `ConnectionRefusedError` and the Web UI shows `attempt` increasing; after `pkill -CONT mailpit`, the final inbox has all 20 expected messages and at least one activity history shows `attempt >= 3`.
- Each assignment's "expected output" block matches what the learner actually sees within reasonable variance.
- The track passes Instruqt's own validation (`instruqt track validate`) and publishes cleanly.

---

# Implementation Specification

The rest of this document is the implementation-ready spec. An implementation agent should be able to build the track from the artifacts below without re-asking design questions.

## Target repository structure

After implementation, `python/standalone-activities/` should look like:

```
python/standalone-activities/
├── PRD.md                              # this document
├── README.md                           # short pointer to PRD + how to develop locally
├── instruqt/                           # Instruqt track source
│   ├── track.yml
│   ├── track_scripts/
│   │   ├── setup-instruqt              # boots temporal dev server + mailpit
│   │   └── cleanup-instruqt
│   ├── 00-prologue/
│   │   ├── challenge.yml
│   │   ├── assignment.md
│   │   ├── setup-workshop
│   │   ├── solve-workshop
│   │   └── check-workshop
│   ├── 01-define-and-execute/
│   │   ├── challenge.yml
│   │   ├── assignment.md
│   │   ├── setup-workshop
│   │   ├── solve-workshop
│   │   └── check-workshop
│   ├── 02-bulk-fanout/                 # same 5-file pattern
│   ├── 03-durability-kill-worker/
│   ├── 04-retries-and-mailpit-down/
│   ├── 05-heartbeats-and-cancel/
│   ├── 06-task-queue-isolation/
│   └── 07-recap-and-next/
├── sandbox/
│   └── Dockerfile                      # the sandbox base image
├── course-repo/                        # the contents that get pre-cloned to /opt/workshop
│   ├── pyproject.toml
│   ├── data/
│   │   └── contacts.csv                # 200 fake recipients
│   ├── scripts/
│   │   ├── kill-worker.sh
│   │   ├── restart-worker.sh
│   │   ├── stop-mailpit.sh             # pkill -STOP mailpit
│   │   └── start-mailpit.sh            # pkill -CONT mailpit
│   ├── exercise/                       # starter code per challenge (with TODO markers)
│   │   ├── 00-prologue/
│   │   ├── 01-define-and-execute/
│   │   └── ... (one folder per challenge)
│   └── solution/                       # completed code per challenge
│       ├── 00-prologue/
│       ├── 01-define-and-execute/
│       └── ... (one folder per challenge)
└── docs/
    └── design-notes.md                 # rationale that didn't fit in PRD
```

## Sandbox Docker image (`sandbox/Dockerfile`)

```dockerfile
FROM python:3.13-slim

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PATH="/root/.local/bin:/usr/local/bin:${PATH}"

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl ca-certificates git tmux procps jq \
    && rm -rf /var/lib/apt/lists/*

# uv (pinned)
RUN curl -LsSf https://astral.sh/uv/install.sh | sh

# Prerelease Temporal CLI with standalone-activity support
ARG TEMPORAL_CLI_TAG=v1.6.2-standalone-activity
RUN curl -L -o /tmp/temporal.tgz \
      "https://github.com/temporalio/cli/releases/download/${TEMPORAL_CLI_TAG}/temporal_cli_$(echo ${TEMPORAL_CLI_TAG#v})_linux_amd64.tar.gz" \
    && tar -xzf /tmp/temporal.tgz -C /usr/local/bin temporal \
    && rm /tmp/temporal.tgz \
    && chmod +x /usr/local/bin/temporal

# MailPit
ARG MAILPIT_VERSION=v1.20.0
RUN curl -L -o /tmp/mailpit.tgz \
      "https://github.com/axllent/mailpit/releases/download/${MAILPIT_VERSION}/mailpit-linux-amd64.tar.gz" \
    && tar -xzf /tmp/mailpit.tgz -C /usr/local/bin mailpit \
    && rm /tmp/mailpit.tgz \
    && chmod +x /usr/local/bin/mailpit

# Pre-clone course repo content (copied in at build time)
COPY course-repo/ /opt/workshop/
RUN cd /opt/workshop && uv sync --frozen

EXPOSE 1025 7233 8025 8233

CMD ["sleep", "infinity"]
```

(Implementer note: pin `TEMPORAL_CLI_TAG` and `MAILPIT_VERSION` to current valid releases at build time; the values above are illustrative.)

## Instruqt `track.yml`

```yaml
slug: python-standalone-activities
id: REGENERATE_ON_FIRST_PUSH
type: track
title: "Bulk Email at Scale: Python Standalone Activities with Temporal"
teaser: Learn Temporal's lightest-weight durability primitive by building a resilient bulk email sender in Python.
description: |
  Standalone activities let you get Temporal's durability, retries, and visibility
  for one-shot jobs without writing a workflow. You will build a bulk email sender,
  watch a worker crash mid-batch and resume, and tune retry policies against a
  flaky SMTP server.
icon: https://temporal.io/temporal.svg
tags: [python, standalone-activities, activities, temporal, durability, retries]
owner: temporal
developers: [nikolay.advolodkin@temporal.io]
timelimit: 7200       # 2 hours
idle_timeout: 1800    # 30 min
maintenance: false
private: false

show_timer: true
lab_config:
  layout: assignment-right
  theme: dark
  sidebar:
    enabled: true
    width: 40

checksum: REGENERATE_ON_FIRST_PUSH

challenges:
  - 00-prologue
  - 01-define-and-execute
  - 02-bulk-fanout
  - 03-durability-kill-worker
  - 04-retries-and-mailpit-down
  - 05-heartbeats-and-cancel
  - 06-task-queue-isolation
  - 07-recap-and-next
```

## Challenge config template (`<challenge>/challenge.yml`)

```yaml
slug: 01-define-and-execute
id: REGENERATE_ON_FIRST_PUSH
type: challenge
title: Your first standalone activity
teaser: Define a @activity.defn, register it in a worker, and fire it directly from a client.
notes:
  - type: text
    contents: See assignment.md
tabs:
  - title: Editor
    type: code
    hostname: workshop
    path: /root/workshop
  - title: Terminal
    type: terminal
    hostname: workshop
  - title: Worker
    type: terminal
    hostname: workshop
  - title: MailPit
    type: website
    url: http://localhost:8025
  - title: Temporal UI
    type: website
    url: http://localhost:8233
difficulty: basic
timelimit: 900   # 15 min cap; expected ~12 min
```

## Track-level scripts

`track_scripts/setup-instruqt`:
```bash
#!/usr/bin/env bash
set -euo pipefail

mkdir -p /var/log/workshop

nohup temporal server start-dev \
  --ui-port 8233 \
  --db-filename /tmp/temporal.db \
  --log-level warn \
  > /var/log/workshop/temporal.log 2>&1 &

nohup mailpit \
  --smtp 0.0.0.0:1025 \
  --listen 0.0.0.0:8025 \
  > /var/log/workshop/mailpit.log 2>&1 &

# Wait for both ports
for i in {1..30}; do
  if (echo > /dev/tcp/localhost/7233) 2>/dev/null \
     && (echo > /dev/tcp/localhost/1025) 2>/dev/null \
     && (echo > /dev/tcp/localhost/8025) 2>/dev/null; then
    break
  fi
  sleep 1
done

mkdir -p /root/workshop
cp -r /opt/workshop/exercise/00-prologue/. /root/workshop/
cp /opt/workshop/pyproject.toml /root/workshop/
cp -r /opt/workshop/data /root/workshop/
cp -r /opt/workshop/scripts /root/workshop/
chmod +x /root/workshop/scripts/*.sh
cd /root/workshop && uv sync
```

`track_scripts/cleanup-instruqt`:
```bash
#!/usr/bin/env bash
pkill -f "temporal server" || true
pkill -f "mailpit" || true
```

## Per-challenge script patterns

`setup-workshop` (template):
```bash
#!/usr/bin/env bash
set -euo pipefail
# Copy fresh starter files for this challenge without clobbering edited ones.
CHALLENGE_DIR=/opt/workshop/exercise/<NN-slug>
rsync -a --ignore-existing "${CHALLENGE_DIR}/" /root/workshop/
# Reset MailPit inbox for a clean slate.
curl -fsS -X DELETE http://localhost:8025/api/v1/messages || true
# Restart MailPit if it's paused from a prior challenge.
pkill -CONT mailpit || true
```

`solve-workshop` (template):
```bash
#!/usr/bin/env bash
set -euo pipefail
SOLUTION_DIR=/opt/workshop/solution/<NN-slug>
cp -rf "${SOLUTION_DIR}/." /root/workshop/
```

`check-workshop` (template — each challenge customizes the assertion block):
```bash
#!/usr/bin/env bash
set -euo pipefail
cd /root/workshop

# Ensure a worker is running.
pkill -f "python -m bulkmail.worker" || true
nohup uv run python -m bulkmail.worker > /tmp/worker.log 2>&1 &
sleep 2

# Reset inbox.
curl -fsS -X DELETE http://localhost:8025/api/v1/messages || true

# Run the learner's starter.
uv run python -m bulkmail.<starter_module>

# Poll MailPit until expected count, or timeout.
EXPECTED=<N>
for i in {1..60}; do
  COUNT=$(curl -fsS http://localhost:8025/api/v1/messages | jq '.messages_count')
  if [ "${COUNT}" -ge "${EXPECTED}" ]; then break; fi
  sleep 1
done
[ "${COUNT}" -ge "${EXPECTED}" ] || { echo "Expected ${EXPECTED} messages, found ${COUNT}"; exit 1; }

# Challenge-specific assertions (Temporal activity list, attempt counts, etc.)
# ...
```

## Source code — `solution/` tree

The implementer should populate `course-repo/solution/<NN-slug>/` with the full source for each step. `course-repo/exercise/<NN-slug>/` is the same tree with key implementations replaced by `# TODO` markers that the assignment.md walks the learner through filling in.

### `pyproject.toml`

```toml
[project]
name = "bulkmail"
version = "0.1.0"
requires-python = ">=3.13"
dependencies = [
  "temporalio>=1.18",
]

[tool.uv]
dev-dependencies = []

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"

[tool.hatch.build.targets.wheel]
packages = ["src/bulkmail"]
```

### `src/bulkmail/__init__.py`

Empty file (package marker).

### `src/bulkmail/shared.py`

```python
from dataclasses import dataclass

TASK_QUEUE_EMAIL = "email-task-queue"
TASK_QUEUE_MARKETING = "email-marketing"
TASK_QUEUE_TRANSACTIONAL = "email-transactional"

SMTP_HOST = "localhost"
SMTP_PORT = 1025

@dataclass
class EmailRequest:
    to: str
    subject: str
    body: str
    sender: str = "noreply@bulkmail.local"
```

### `src/bulkmail/activities.py`

```python
import smtplib
import time
from email.message import EmailMessage

from temporalio import activity

from .shared import EmailRequest, SMTP_HOST, SMTP_PORT


@activity.defn
def send_email(req: EmailRequest) -> str:
    activity.logger.info("Sending email to %s", req.to)
    msg = EmailMessage()
    msg["From"] = req.sender
    msg["To"] = req.to
    msg["Subject"] = req.subject
    msg.set_content(req.body)

    with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=5) as smtp:
        smtp.send_message(msg)

    return f"sent:{req.to}"


@activity.defn
def send_email_batch(reqs: list[EmailRequest]) -> int:
    # Resume from last heartbeat if this is a retry.
    start_index = activity.info().heartbeat_details[0] if activity.info().heartbeat_details else 0
    activity.logger.info("Batch resuming at index %d of %d", start_index, len(reqs))

    sent = start_index
    for i in range(start_index, len(reqs)):
        msg = EmailMessage()
        msg["From"] = reqs[i].sender
        msg["To"] = reqs[i].to
        msg["Subject"] = reqs[i].subject
        msg.set_content(reqs[i].body)
        with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=5) as smtp:
            smtp.send_message(msg)
        sent += 1
        time.sleep(0.05)
        activity.heartbeat(sent)

    return sent
```

### `src/bulkmail/worker.py`

```python
import asyncio
import os
from concurrent.futures import ThreadPoolExecutor

from temporalio.client import Client
from temporalio.worker import Worker

from .activities import send_email, send_email_batch
from .shared import TASK_QUEUE_EMAIL


async def main():
    task_queue = os.environ.get("TASK_QUEUE", TASK_QUEUE_EMAIL)
    client = await Client.connect("localhost:7233")
    worker = Worker(
        client,
        task_queue=task_queue,
        activities=[send_email, send_email_batch],
        activity_executor=ThreadPoolExecutor(5),
    )
    print(f"Worker running on task queue '{task_queue}'")
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
```

### `src/bulkmail/worker_transactional.py`

Same as `worker.py` but with `task_queue=TASK_QUEUE_TRANSACTIONAL` hardcoded — used in challenge 06.

### `src/bulkmail/send_one.py`

```python
import asyncio
from datetime import timedelta

from temporalio.client import Client

from .activities import send_email
from .shared import EmailRequest, TASK_QUEUE_EMAIL


async def main():
    client = await Client.connect("localhost:7233")
    result = await client.execute_activity(
        send_email,
        args=[EmailRequest(
            to="alice@example.com",
            subject="Welcome!",
            body="Glad to have you with us.",
        )],
        id="send-email-alice",
        task_queue=TASK_QUEUE_EMAIL,
        start_to_close_timeout=timedelta(seconds=10),
    )
    print(f"Activity result: {result}")


if __name__ == "__main__":
    asyncio.run(main())
```

### `src/bulkmail/send_bulk.py`

```python
import asyncio
import csv
from datetime import timedelta
from pathlib import Path

from temporalio.client import Client
from temporalio.common import RetryPolicy

from .activities import send_email
from .shared import EmailRequest, TASK_QUEUE_EMAIL

CONTACTS_PATH = Path("data/contacts.csv")


def load_contacts(limit: int | None = None) -> list[EmailRequest]:
    with CONTACTS_PATH.open() as f:
        rows = list(csv.DictReader(f))
    if limit:
        rows = rows[:limit]
    return [
        EmailRequest(
            to=row["email"],
            subject=f"Newsletter #{i + 1}",
            body=f"Hi {row['name']}, here's your weekly update.",
        )
        for i, row in enumerate(rows)
    ]


async def main(count: int = 200):
    client = await Client.connect("localhost:7233")
    reqs = load_contacts(limit=count)

    handles = []
    for i, req in enumerate(reqs):
        handle = await client.start_activity(
            send_email,
            args=[req],
            id=f"email-{i:04d}",
            task_queue=TASK_QUEUE_EMAIL,
            start_to_close_timeout=timedelta(seconds=10),
            retry_policy=RetryPolicy(
                initial_interval=timedelta(seconds=1),
                backoff_coefficient=2.0,
                maximum_interval=timedelta(seconds=10),
                maximum_attempts=0,  # unlimited
            ),
        )
        handles.append(handle)

    results = await asyncio.gather(*(h.result() for h in handles))
    print(f"Sent {len(results)} emails")


if __name__ == "__main__":
    import sys
    asyncio.run(main(int(sys.argv[1]) if len(sys.argv) > 1 else 200))
```

### `src/bulkmail/cancel_demo.py`

```python
import asyncio
from datetime import timedelta

from temporalio.client import Client

from .activities import send_email_batch
from .shared import EmailRequest, TASK_QUEUE_EMAIL


async def main():
    client = await Client.connect("localhost:7233")
    reqs = [
        EmailRequest(to=f"user{i}@example.com", subject="Digest", body="Body")
        for i in range(100)
    ]

    handle = await client.start_activity(
        send_email_batch,
        args=[reqs],
        id="batch-digest",
        task_queue=TASK_QUEUE_EMAIL,
        start_to_close_timeout=timedelta(minutes=2),
        heartbeat_timeout=timedelta(seconds=2),
    )

    await asyncio.sleep(3)
    await handle.cancel()
    try:
        await handle.result()
    except Exception as e:
        print(f"Activity cancelled (partial progress expected): {e}")


if __name__ == "__main__":
    asyncio.run(main())
```

## Course data — `data/contacts.csv`

Header + 200 rows:
```
email,name
user001@example.com,User 001
user002@example.com,User 002
...
user200@example.com,User 200
```

(Implementer: generate programmatically; no real PII.)

## Chaos scripts — `scripts/`

`kill-worker.sh`:
```bash
#!/usr/bin/env bash
pkill -f "python -m bulkmail.worker" || true
echo "Worker killed."
```

`restart-worker.sh`:
```bash
#!/usr/bin/env bash
cd /root/workshop
nohup uv run python -m bulkmail.worker > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
```

`stop-mailpit.sh`:
```bash
#!/usr/bin/env bash
pkill -STOP mailpit
echo "MailPit paused (SIGSTOP). Inbox preserved; SMTP accept loop frozen."
```

`start-mailpit.sh`:
```bash
#!/usr/bin/env bash
pkill -CONT mailpit
echo "MailPit resumed (SIGCONT)."
```

## Assignment.md format

Every `assignment.md` follows this strict template:

````markdown
# <Challenge title>

## Concept (5 min)

<600–800 words. One core idea. One diagram or code-shape illustration. One analogy.
No commands to run in this section.>

## Hands-on (5–7 min)

1. **<Action verb>** — concrete instruction.
   ```bash
   <exact command or code edit>
   ```
   **Expected output:**
   ```
   <literal expected snippet>
   ```

2. <next step>

3. <next step>

When all steps pass their expected output, press **Check**.
````

### Fully-written gold-standard sample — `01-define-and-execute/assignment.md`

````markdown
# Your first standalone activity

## Concept (5 min)

In Temporal, an **activity** is just a Python function the platform promises to
run reliably. It will be retried if it crashes, persisted if your worker dies,
and visible in the Web UI from the moment you start it to the moment it finishes.

What's new about *standalone* activities is that **no workflow has to exist**
to run one. You don't have to write `@workflow.defn`. You don't have to think
about replay-safety, determinism, or signals. You just decorate a function and
ask the Temporal client to run it.

```text
┌─────────────┐    execute_activity     ┌────────────────┐    poll      ┌─────────┐
│   client    │ ──────────────────────► │ Temporal server│ ◄────────────│  worker │
│ (your app)  │                         │  (durable log) │              │ (you)   │
└─────────────┘                         └────────────────┘              └─────────┘
                                                                              │
                                                                              ▼
                                                                       ┌──────────────┐
                                                                       │  MailPit     │
                                                                       │  (SMTP)      │
                                                                       └──────────────┘
```

The three pieces:

- **Activity definition** — a plain Python function annotated with
  `@activity.defn`. Inputs and outputs are serialized (we use a dataclass).
- **Worker** — a process that polls a *task queue* for work and runs activity
  functions. You start it once and leave it running.
- **Client call** — `client.execute_activity(fn, args=..., id=..., task_queue=...,
  start_to_close_timeout=...)`. The client doesn't run the function; it asks
  Temporal to schedule it. Temporal hands the work to a worker. The result
  comes back to the client.

Think of it like a typed durable job queue: the client enqueues, the worker
dequeues, Temporal keeps the receipt.

Two details that always come up:

- **Task queue** is a routing key, not a permission boundary. Workers poll
  specific queues; activities are scheduled to specific queues. We'll use
  `email-task-queue` throughout this track.
- **`start_to_close_timeout`** is the per-attempt deadline. If a single
  attempt at the activity takes longer than this, Temporal cancels it and
  retries (according to the retry policy, which defaults to unlimited
  attempts with exponential backoff).

By the end of this challenge you'll have all three pieces wired up and one
real email landing in the MailPit inbox tab.

## Hands-on (5–7 min)

1. **Open `src/bulkmail/activities.py`** and fill in the `send_email` function.
   The TODO comment shows you where:

   ```python
   @activity.defn
   def send_email(req: EmailRequest) -> str:
       # TODO: build an EmailMessage with req.sender, req.to, req.subject, req.body
       # TODO: send it via smtplib.SMTP("localhost", 1025)
       # TODO: return f"sent:{req.to}"
   ```

   Use `email.message.EmailMessage` and `smtplib.SMTP`. Both are in the
   standard library — no new imports needed beyond what the file already has.

2. **Start the worker** in the **Worker** terminal tab:

   ```bash
   cd /root/workshop
   uv run python -m bulkmail.worker
   ```

   **Expected output:**
   ```
   Worker running on task queue 'email-task-queue'
   ```

3. **Run the client** in the **Terminal** tab:

   ```bash
   cd /root/workshop
   uv run python -m bulkmail.send_one
   ```

   **Expected output:**
   ```
   Activity result: sent:alice@example.com
   ```

4. **Open the MailPit tab.** You should see exactly one message:
   - From: `noreply@bulkmail.local`
   - To: `alice@example.com`
   - Subject: `Welcome!`

5. **Open the Temporal UI tab** and navigate to the **Activities** view.
   You should see `send-email-alice` with status `Completed` and attempt `1`.

When MailPit shows the message and the Temporal UI shows the completed
activity, press **Check**.
````

(Implementer: write each other challenge's `assignment.md` to the same template and length. The `Concept` section for each challenge is summarized in the table below; the `Hands-on` section follows the implementation manifest.)

## Per-challenge implementation manifest

For each challenge, this table specifies what changes from the prior state. The implementer uses these to author `exercise/`, `solution/`, and the per-challenge scripts.

| # | Concept focus | Files added/changed in `solution/<NN>/` | Check assertion |
|---|---|---|---|
| 00 | Scenario + panes + standalone vs. workflow | None (read-only tour) | `curl http://localhost:8025/api/v1/info` returns 200; `temporal operator cluster health` exits 0 |
| 01 | `@activity.defn`, worker registration, `client.execute_activity`, task queue, `start_to_close_timeout` | `src/bulkmail/{activities.py,worker.py,send_one.py,shared.py}` | MailPit has 1 message to `alice@example.com` with subject `Welcome!`; `temporal activity list` shows 1 `Completed` |
| 02 | `start_activity` + handle, `asyncio.gather`, ThreadPoolExecutor concurrency cap | Add `src/bulkmail/send_bulk.py` (50 recipients); learner edits `worker.py` if needed (no change in v1) | MailPit has 50 messages with subjects `Newsletter #1`..`Newsletter #50`; `temporal activity list` shows 50 `Completed` |
| 03 | Server-side state durability, default RetryPolicy, "worker is cattle" | `send_bulk.py` bumped to 200 recipients; `activities.py` gains `time.sleep(0.5)` so wall-clock is perceptible (setup script edits this for the learner) | MailPit ends with 200 unique messages; `temporal activity list --query "ExecutionStatus='Completed' AND TaskQueue='email-task-queue'"` returns 200; at least one history has `attempt >= 2` |
| 04 | `RetryPolicy` fields, `start_to_close_timeout` vs `schedule_to_close_timeout`, retries are server-driven | `send_bulk.py` learner adds explicit `RetryPolicy(...)` kwargs; 20-recipient variant `send_bulk_small.py` for this challenge | MailPit ends with 20 messages; at least one history has `attempt >= 3` (caused by `pkill -STOP mailpit` step) |
| 05 | Heartbeats, `heartbeat_timeout`, `handle.cancel()`, `heartbeat_details` on retry | Add `send_email_batch` to `activities.py`; add `cancel_demo.py` | MailPit has between 20 and 80 messages (partial); activity final event is `ActivityTaskCancelled`; heartbeat payload visible |
| 06 | Task queue as routing key, isolation patterns | Add `worker_transactional.py`; modify `send_bulk.py` to route based on a `priority` arg | After killing marketing worker: transactional 5 messages arrive in MailPit; marketing 5 stay pending in `temporal activity list`; after restart: total 10 messages |
| 07 | Recap, when to use workflows instead, pointer to docs | None (recap only) | Learner runs `temporal activity list` (sees ~280+ rows from prior challenges) and answers 2 multiple-choice questions inline in assignment.md |

## Knowledge check (recap)

Embedded inline in `07-recap-and-next/assignment.md`:

1. *Your worker crashes mid-batch. Where does the in-flight activity state live?*
   - a) In the worker's RAM (lost on crash)
   - b) In the Temporal server (durable, replayed to next worker) ✓
   - c) In the client process (must stay running until completion)

2. *Which is true about a `RetryPolicy` on a standalone activity?*
   - a) It must be defined on a workflow.
   - b) It's passed as a kwarg on `execute_activity` / `start_activity`, and the server drives the retry loop. ✓
   - c) It only applies to local activities.

## Bootstrap steps for the implementation agent

When this PRD is approved, the agent should:

1. Work inside `python/standalone-activities/` of this repo (where this PRD lives).
2. `mkdir -p instruqt sandbox course-repo/{data,scripts,src/bulkmail,exercise,solution} docs`.
3. Create each of the files specified in the **Target repository structure**, **Sandbox Docker image**, **track.yml**, **Track-level scripts**, **Source code**, **Course data**, and **Chaos scripts** sections.
4. For each of the 8 challenges, create the 5-file folder (`challenge.yml`, `assignment.md`, `setup-workshop`, `solve-workshop`, `check-workshop`) using the templates and the per-challenge manifest.
5. Author `exercise/<NN>/` for each challenge by copying the corresponding `solution/<NN>/` and replacing the key implementations with `# TODO:` markers consistent with what the assignment teaches.
6. Build the sandbox image locally (`docker build -t standalone-activities-sandbox sandbox/`) and run it interactively to verify Temporal server + MailPit boot cleanly.
7. Run each challenge's `check-workshop` against the `solution/` overlay to verify checks pass on a known-good state.
8. Commit (do not push) and surface the new branch for the user to review.
