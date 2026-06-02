# Skip the workflow

Most Temporal tutorials show you Activities running inside Workflows. This module flips that: you'll write one Activity and run it **without** a Workflow at all — directly from a Temporal Client. Then you'll run the same Activity wrapped in a Workflow and see exactly what that wrapping costs you.

By the end you'll be able to:

- Invoke an Activity from a client with no Workflow class.
- Compare a Standalone Activity vs. an Activity-in-Workflow on **events, actions, retention, latency, and throughput**.
- Decide when the wrapping is worth it — and when it isn't.

Budget ~10 minutes.

---

## 1. Write the activity (~2 min)

Open `src/webhooks/activities.py` in the **Editor** tab. You'll see a `deliver_webhook` function with three TODOs:

```python
@activity.defn
def deliver_webhook(req: WebhookDelivery) -> int:
    activity.logger.info("Delivering webhook for event %s to %s", req.event_id, req.url)
    # TODO: POST req.payload to req.url using httpx.post()
    # TODO: raise on non-2xx response (response.raise_for_status())
    # TODO: return the HTTP status code
    raise NotImplementedError("Fill in deliver_webhook")
```

Fill it in. `httpx` is already in your environment.

> **What's happening:** This is a regular `@activity.defn`. Nothing here screams "standalone" yet. Standalone Activities use the same activity definition as Workflow-bound ones — that's the point. The standalone-ness is in how you call it, not how you define it.

---

## 2. Run it standalone (~2 min)

In the **Worker** tab, start the worker:

```bash
cd /root/workshop
uv run python -m webhooks.worker
```

Expected:

```
Worker running on task queue 'webhook-queue'
```

In the **Terminal** tab, fire one delivery as a Standalone Activity:

```bash
uv run python -m webhooks.send_standalone evt_001
```

Expected:

```
Standalone activity completed with status 200
```

Open the **Echo server** tab. You'll see one delivery in the JSON.

> **What's happening:** Look at `send_standalone.py`. The whole call is `await client.execute_activity(deliver_webhook, ...)`. **No `@workflow.defn` anywhere in your code.** The client tells Temporal "schedule this activity"; Temporal hands it to your worker; the result comes back. It's a typed durable job queue.

---

## 3. Run the same activity inside a workflow (~2 min)

A 5-line `WebhookWorkflow` is provided in `src/webhooks/workflow.py`. You don't need to edit it — it just wraps `deliver_webhook` and calls it via `workflow.execute_activity`. Open it and read it once so you see the shape.

Fire one delivery through the workflow:

```bash
uv run python -m webhooks.send_via_workflow evt_002
```

Expected:

```
Workflow completed with activity returning status 200
```

Refresh the **Echo server** tab. You should now see **2** deliveries total — one per call.

> **What's happening:** Same Activity. Same business outcome. But the second one was scheduled inside a Workflow execution. Both are durable. Both are retried on failure (no failures today). Both show up in the Temporal UI. The difference shows up in how much Temporal had to record to make that happen.

---

## 4. Compare the cost (~3 min)

The two ways look identical from the outside — but Temporal did very different amounts of work under the hood. Run these side by side in the **Terminal** tab:

```bash
# Standalone Activity — look for "StateTransitionCount: 3"
temporal activity describe --address localhost:7233 --activity-id deliver-evt_001

# Activity-in-Workflow — count the rows in "Progress" (11 events)
temporal workflow show --address localhost:7233 --workflow-id wf-evt_002
```

You should see something like:

| | Standalone Activity | Activity-in-Workflow |
|---|---|---|
| State transitions / events | **3** (per `StateTransitionCount` in `activity describe`) | **11** (`WorkflowExecutionStarted` → `WorkflowTaskScheduled` → ... → `WorkflowExecutionCompleted`) |
| Temporal Cloud actions billed* | 1 | ≥ 2 |
| History retention | Activity-scoped | Workflow-scoped (full history retained) |
| Visibility | `temporal activity list/describe` | Full workflow timeline + search |
| Latency overhead | Lower (no workflow scheduling) | Higher (workflow tasks bracket the activity) |
| Throughput at scale | Higher (fewer events per unit of work) | Lower (more events per unit of work) |

\* Approximate; check current Temporal Cloud pricing for the exact billing model.

Also open the **Temporal UI** tab and click into both executions. The workflow view has a full timeline with task events around the activity; the standalone view has just the activity itself.

> **What's happening:** Wrapping an Activity in a Workflow gives you orchestration — signals, queries, child workflows, multi-step state. If you don't need any of that, you're paying the wrapping cost (events, actions, retention, latency) for nothing. Standalone is the right shape for one-shot durable work.

---

## Check

When the comparison feels intuitive, press **Check**. The check validates:

- The echo server received both deliveries.
- A standalone activity ran on `webhook-queue`.
- A workflow ran on `webhook-queue`.

If the check fails, peek at `/tmp/worker.log` and rerun the two `send_*` commands.

---

## Coming up

The next modules tackle what happens when reality intrudes:

- **Module 02** — Idempotency and crash safety. Crash the worker mid-delivery; watch the echo server show 2 deliveries; fix it.
- **Module 03** — Concurrency, rate limits, priority & fairness. Stop one loud tenant from starving the rest.
- **Module 04** — Dedup via ID reuse. Same upstream event arrives twice; let Temporal reject the duplicate.
- **Module 05** (optional) — Ship the same code against your Temporal Cloud namespace.
- **Module 06** — When SAA vs. when Workflow. Three scenarios, your call.
