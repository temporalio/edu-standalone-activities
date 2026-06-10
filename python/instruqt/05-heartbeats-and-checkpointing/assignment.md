---
slug: heartbeats-and-checkpointing
id: uzjyqxsnftd8
type: challenge
title: Heartbeats and checkpointing
teaser: Resume a long-running Standalone Activity from where it left off after a Worker
  crash.
notes:
- type: text
  contents: |
    # Heartbeats and checkpointing

    A single Standalone Activity that processes a batch of webhook deliveries can take minutes. When the Worker crashes mid-batch, traditional job queues either lose all in-flight progress or expect *you* to invent a checkpointing scheme per job type — and most of those schemes leak state into a sidecar database nobody wants to maintain.

    Standalone Activities have heartbeats built in. The Activity calls `activity.heartbeat(progress)` after each unit of work; the Temporal server stores that value. If the attempt dies (Worker crash, machine reboot, deploy), the next attempt reads `activity.info().heartbeat_details` and resumes from the last reported checkpoint instead of redoing work.

    ## What you'll do

    1. Run a long-running Activity that delivers 10 webhooks. Kill the Worker mid-batch. Watch the retry start from item 0 — the receiver records duplicates of items already delivered.
    2. Add one line to read `heartbeat_details` on retry. Re-run. Kill again. Watch the retry pick up where it left off — no duplicates.
tabs:
- id: pxzal47h0ue3
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/05-heartbeats-and-checkpointing/exercise
- id: o7wtppqqzxxy
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/05-heartbeats-and-checkpointing/solution
- id: pixhhhhpmchi
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/05-heartbeats-and-checkpointing/exercise
- id: 0le9qjmg7tpy
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/05-heartbeats-and-checkpointing/exercise
- id: a9zpxxxduxi2
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: f9litgnhwwoy
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Resume long-running jobs from the last checkpoint

Traditional job queues vanish in-flight work when the Worker crashes. For a 30-second job that's annoying; for a 30-minute batch that's already half done, it's a real cost — and the typical fix is to invent a per-job-type checkpointing scheme that lives in a sidecar database.

Standalone Activities include heartbeats and checkpointing at the platform layer. `activity.heartbeat(progress)` reports liveness *and* stores arbitrary progress data on the Temporal server. When the next attempt starts, it reads `activity.info().heartbeat_details` and resumes from there. No sidecar.

You'll do three things in this module:

1. Run a 10-item batch delivery Activity. Kill the Worker mid-batch. Watch the retry start from item 0 — the receiver records duplicates.
2. Add one block to read `heartbeat_details` on retry and skip items already delivered.
3. Re-run, kill again, watch the retry resume from the checkpoint — no duplicates.

The **Solution** tab has the finished code. Estimated time: 10 minutes.

---

## 1. See the bug: retry restarts from item 0 (~3 min)

Open `src/webhooks/activities.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. The Activity already calls `activity.heartbeat(delivered)` after each item — so the server *has* the progress data. What's missing is the read on retry.

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send a 10-item batch and immediately kill the Worker while it's running:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_batch 10 &
sleep 4 && scripts/kill-worker.sh
```

That sequence:
- Submits a batch of 10 items (the Activity sleeps 1s between each — total ~10s).
- Waits 4 seconds (so ~4 items are delivered), then kills the Worker.
- Leaves the `send_batch` client waiting in the background for the retry to finish.

### Observe the state while the Worker is down

Before you restart the Worker, take a look at where things stand:

- Click the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You'll see **about 4 deliveries** — the items that landed before the kill. Real partial progress, sitting at the receiver.
- Click the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** → click into `deliver-batch-10`. The Activity is still listed as a running Standalone Activity. Temporal hasn't given up on it — it's waiting for a Worker to come back and finish the job. **It stays in this in-flight state until you restart the Worker.**

Now restart the Worker so the retry has somewhere to run:

```bash,run
uv run python -m webhooks.worker
```

Return to the [button label="Terminal" background="#444CE7"](tab-2) tab and wait for the background client to finish:

```bash,run
wait
```

In about 5 seconds, `heartbeat_timeout` fires on the server (no heartbeat for 5s = attempt is dead), Temporal triggers a retry, and the new Worker picks it up. The retry replays the Activity body **from the top** — including items already delivered.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see **14+ deliveries** for a 10-item batch: items 0–3 (or however many got through before the kill) are recorded *twice*. The receiver had no way to know these were duplicates because each carries a different `event_id`.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** → find `deliver-batch-10`. You'll see two attempts: the first one timed out, the second one completed.

> **What's happening:** the Activity heartbeated its progress on the first attempt, but the second attempt never reads `heartbeat_details`. So it starts `start_index = 0` and redoes everything.

---

## 2. Read the checkpoint on retry (~2 min)

Back in the [button label="Exercise" background="#444CE7"](tab-0) tab, find the `TODO` in `deliver_webhook_batch`. Replace:

```python
start_index = 0
```

with:

```python
start_index = 0
if info.heartbeat_details:
    start_index = info.heartbeat_details[0]
    activity.logger.info(
        "Resuming from index %d (attempt %d)", start_index, info.attempt
    )
```

That's the fix. The full solution is in the **Solution** tab.

`info.heartbeat_details` is a tuple of whatever you passed to `activity.heartbeat()` in the previous attempt. We pass a single int (`delivered`), so `heartbeat_details[0]` is the count of items already done.

---

## 3. Verify the fix (~3 min)

Restart the Worker so it picks up the new code. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then re-run:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, repeat the kill-mid-batch dance:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_batch 10 &
sleep 4 && scripts/kill-worker.sh
```

### Observe the state while the Worker is down (again)

Same drill as section 1 — peek before restarting:

- Click the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. **About 4 deliveries** so far (items 0–3 or thereabouts). The first attempt heartbeated its progress to the server before it died.
- Click the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** → `deliver-batch-10`. The Activity is still in flight, waiting for a Worker. **It will stay in this state until you restart the Worker** — and this time, when the retry runs, it'll read `heartbeat_details` and pick up from item 4 instead of item 0.

Restart the Worker:

```bash,run
uv run python -m webhooks.worker
```

Return to the [button label="Terminal" background="#444CE7"](tab-2) tab and wait for the background client to finish:

```bash,run
wait
```

In the [button label="Webhook receiver" background="#444CE7"](tab-4) tab, count climbs to exactly **10**. The retry read `heartbeat_details`, jumped to the checkpoint index, and finished the remaining items without redoing anything.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** → `deliver-batch-10`. Click into the second attempt: you'll see its log line "Resuming from index N (attempt 2)" confirming the checkpoint was read.

> **The takeaway:** the same Activity, the same `kill-worker.sh`, the same restart — but the receiver sees each item exactly once. Heartbeating is the durable-job-queue version of "save your progress before the next crash."

---

## Check your understanding

> Your batch Activity has `heartbeat_timeout=5s` and processes one item per second. Mid-batch, the Worker hangs (deadlock, not crash) — it stops calling heartbeat but the process is still alive. What does Temporal do?

<details>
<summary>Answer</summary>

Temporal treats the attempt as dead after 5 seconds with no heartbeat — same as a crash. It schedules a retry on whatever Worker picks it up next.

That's the point of `heartbeat_timeout`: it's the *server's* way to detect a stuck or dead attempt without waiting for the (much longer) `start_to_close_timeout`. Heartbeats are not just for storing progress — they're the liveness signal that lets the server route around a sick Worker quickly.

Pair `activity.heartbeat()` with `heartbeat_timeout` whenever an Activity does work that takes longer than a few seconds. Without `heartbeat_timeout` set, the server only learns about a dead attempt at `start_to_close_timeout`, which might be minutes.

</details>

## Coming up

**Module 06** — Same code runs anywhere. You've now used Standalone Activities for retries, idempotency, dedup, rate limits, and heartbeats. Final stop: take the same Activity code you've been writing and call it from a Workflow — one platform, two job types, zero rewrites.
