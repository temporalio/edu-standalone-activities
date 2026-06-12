---
slug: same-code-runs-anywhere
id: qsxxv9y7a5oh
type: challenge
title: Same code runs anywhere
teaser: The same Activity you've been writing, now called from a Workflow. One Activity,
  two job types.
notes:
- type: text
  contents: |
    # Same code runs anywhere

    Traditional job queues can paint you into a corner. The queue runs jobs, orchestration lives somewhere else, and the code that ran in the queue often gets copied or rewritten when the work becomes a multi-step process.

    Standalone Activities give you a useful path forward. The Activity code you wrote in Modules 01-05 is a self-contained unit of durable work. Today you submit it as a top-level job. Tomorrow, when the job grows into a multi-step process, you can call the same Activity from a Workflow without rewriting it.

    This module proves it. The exact same deliver_webhook function is submitted two ways: directly via client.execute_activity (the Standalone Activity path you've used all tutorial) and via client.execute_workflow (a Workflow that calls the Activity as a step). Same Activity, two job types.
tabs:
- id: ripqsfaeunnx
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/06-same-code-runs-anywhere/exercise
- id: ivz6nmbhlxf9
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/06-same-code-runs-anywhere/solution
- id: vcb9zf4htmkc
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/06-same-code-runs-anywhere/exercise
- id: a3r5xefivotx
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/06-same-code-runs-anywhere/exercise
- id: 2eam4nnuhacf
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: s5duvkctvcpf
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# One Activity, two job types

You've written `deliver_webhook` once. Through five modules you've submitted it as a Standalone Activity and learned how it behaves under the Temporal platform: durably persisted, retried, dedupable, rate-capped, heartbeating.

Now the upgrade path: the same `deliver_webhook` function, unchanged, runs as a step inside a multi-step Workflow. No copy, no rewrite. Temporal handles both job types.

You'll do three things in this module:

1. Read `activities.py`. It's the exact `deliver_webhook` Activity from Module 01.
2. Run `send_standalone.py`, which submits the Activity directly. You've done this 5 modules in a row.
3. Run `send_via_workflow.py`, which submits the same Activity as a Workflow step. Compare the two in the Temporal UI.

Estimated time: 8 minutes.

---

## 1. Look at the Activity (~1 min)

Open `src/webhooks/activities.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. You'll recognize it. It's the `deliver_webhook` Activity you wrote in Module 01:

```python
@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    response = httpx.post(req.url, json=req.payload, timeout=5.0)
    response.raise_for_status()
    return response.status_code
```

Now open `src/webhooks/workflow.py`. A short Workflow that calls the same Activity:

```python
with workflow.unsafe.imports_passed_through():
    from .activities import deliver_webhook   # SAME function

@workflow.defn
class WebhookWorkflow:
    @workflow.run
    async def run(self, req: WebhookDelivery) -> int:
        return await workflow.execute_activity(
            deliver_webhook,
            req,
            start_to_close_timeout=timedelta(seconds=10),
        )
```

The Workflow imports `deliver_webhook` from the same module the standalone caller does. The Activity does not know whether it is being invoked as a Standalone Activity or as a Workflow step. That's the point.

---

## 2. Submit it as a Standalone Activity (~2 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send one standalone delivery:

```bash,run
uv run python -m webhooks.send_standalone evt_001
```

You should see:

```
Standalone Activity completed with status 200
```

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities**. You'll see `deliver-evt_001` listed as a completed Standalone Activity. This is the API surface you've used through all five previous modules.

---

## 3. Submit the same Activity as a Workflow step (~2 min)

In the [button label="Terminal" background="#444CE7"](tab-2) tab, run the second starter:

```bash,run
uv run python -m webhooks.send_via_workflow evt_002
```

You should see:

```
Workflow completed with Activity returning status 200
```

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab. Now look at both views:

- **Standalone Activities** tab: `deliver-evt_001` from step 2 is here.
- **Workflows** tab: `wf-evt_002` is here, and clicking it shows the Activity as a step in its history.

The same `deliver_webhook` Activity ran in both cases. The receiver recorded two distinct deliveries, one per submission. From the Activity's perspective, there is no difference between the two calls.

> **What's happening:** the Worker registered both the Activity *and* the Workflow. Temporal's server routes a `start_activity` call to dispatch the Activity directly; a `start_workflow` call dispatches the Workflow, which then dispatches the Activity. Same code, two submission paths, two job types.

---

## Why this matters

Every Activity you've written in this tutorial is a reusable building block:

- `deliver_webhook` could be step 3 of an order-processing Workflow tomorrow, between "charge the card" and "send confirmation email."
- The `deliver_webhook_batch` from Module 05 could be a long-running step inside a daily-rollup Workflow.
- The `id_conflict_policy`, `max_activities_per_second`, and `Priority` knobs you learned still apply when those Activities are called from inside a Workflow.

This is the upgrade path traditional job queues don't have. When the requirements grow from "one durable POST" to "charge the card, then reserve inventory, then notify ops if any step fails," you compose the Activities you already wrote into a Workflow. You don't rewrite them in another tool.

---

## Coinbase did exactly this

At Replay 2026, Coinbase described migrating their custom Background Jobs Service onto Standalone Activities. That service handles 200 to 600 million jobs per day across 186 namespaces. The same Activity code can run standalone today and be composed into Workflows tomorrow, which lets one system replace a separate job queue and orchestrator.

The full talk: [Standalone Activities for durable job processing, Coinbase at Replay 2026](https://www.youtube.com/watch?v=zsF5Y-IOMOw).

---

## Wrap-up

What you can now do with Standalone Activities in Python:

- **`client.execute_activity` / `client.start_activity`**: submit an Activity as a top-level durable job, no Workflow required.
- **Idempotency keys for external writes**: make retries safe by giving the receiver a stable dedup key, such as the logical webhook event key from Module 02.
- **`ActivityIDConflictPolicy.USE_EXISTING`**: let the server handle duplicate `start_activity` calls instead of erroring.
- **`max_activities_per_second`** on the Worker: cap dispatch rate to protect the downstream service.
- **`Priority(priority_key, fairness_key, fairness_weight)`** on `start_activity`: push urgent work ahead of bulk when the queue is contended.
- **`activity.heartbeat(progress)` + `heartbeat_timeout`**: report progress from long-running jobs and resume from the last checkpoint after a crash.
- **The same Activity, called from a Workflow**: use the Activity in multi-step orchestration without rewriting it.

Temporal lets you start with a job and move to a Workflow when the work grows. The Activity code you wrote still comes with you.

Click **Check** to finish the track.

---

[**Share your feedback**](https://forms.gle/Tvx6YenX7zTHgrqU8)
