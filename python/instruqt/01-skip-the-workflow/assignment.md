---
slug: skip-the-workflow
id: cnt4ivcsp1av
type: challenge
title: Skip the workflow
teaser: Build a webhook delivery activity, run it standalone, run it via a workflow,
  and compare the cost.
notes:
- type: text
  contents: |
    # Standalone Activities as a Job Queue

    You're about to build a durable webhook-delivery job two ways and
    see — with real event counts — why one shape costs less than the
    other.

    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 880 580" font-family="system-ui, -apple-system, 'Segoe UI', sans-serif">
      <rect width="880" height="580" fill="#1a1a2e"/>
      <text x="440" y="36" text-anchor="middle" fill="#e2e8f0" font-size="20" font-weight="600">Standalone Activity vs. Activity-in-Workflow</text>
      <text x="440" y="58" text-anchor="middle" fill="#a0aec0" font-size="13">Same business outcome. Different cost.</text>
      <g transform="translate(40, 90)">
        <rect x="0" y="0" width="380" height="460" fill="none" stroke="#4a5568" stroke-width="1" stroke-dasharray="3 3" rx="6"/>
        <text x="190" y="22" text-anchor="middle" fill="#b794f6" font-size="15" font-weight="600">Standalone Activity</text>
        <rect x="110" y="48" width="160" height="54" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="71" text-anchor="middle" fill="#e2e8f0" font-size="13" font-weight="600">Client</text>
        <text x="190" y="90" text-anchor="middle" fill="#a0aec0" font-size="11" font-family="ui-monospace, monospace">execute_activity(...)</text>
        <line x1="190" y1="102" x2="190" y2="135" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="185,130 190,140 195,130" fill="#a0aec0"/>
        <rect x="50" y="145" width="280" height="138" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="168" text-anchor="middle" fill="#e2e8f0" font-size="13" font-weight="600">Temporal Server</text>
        <text x="190" y="194" text-anchor="middle" fill="#f6e05e" font-size="22" font-weight="700">3 events</text>
        <text x="190" y="222" text-anchor="middle" fill="#a0aec0" font-size="11">ActivityTaskScheduled</text>
        <text x="190" y="240" text-anchor="middle" fill="#a0aec0" font-size="11">ActivityTaskStarted</text>
        <text x="190" y="258" text-anchor="middle" fill="#a0aec0" font-size="11">ActivityTaskCompleted</text>
        <line x1="190" y1="283" x2="190" y2="316" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="185,311 190,321 195,311" fill="#a0aec0"/>
        <rect x="110" y="326" width="160" height="54" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="349" text-anchor="middle" fill="#e2e8f0" font-size="13" font-weight="600">Worker</text>
        <text x="190" y="368" text-anchor="middle" fill="#a0aec0" font-size="11" font-family="ui-monospace, monospace">@activity.defn</text>
        <line x1="190" y1="380" x2="190" y2="413" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="185,408 190,418 195,408" fill="#a0aec0"/>
        <rect x="110" y="423" width="160" height="32" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="444" text-anchor="middle" fill="#e2e8f0" font-size="12">Echo Server</text>
      </g>
      <g transform="translate(460, 90)">
        <rect x="0" y="0" width="380" height="460" fill="none" stroke="#4a5568" stroke-width="1" stroke-dasharray="3 3" rx="6"/>
        <text x="190" y="22" text-anchor="middle" fill="#b794f6" font-size="15" font-weight="600">Activity-in-Workflow</text>
        <rect x="110" y="48" width="160" height="54" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="71" text-anchor="middle" fill="#e2e8f0" font-size="13" font-weight="600">Client</text>
        <text x="190" y="90" text-anchor="middle" fill="#a0aec0" font-size="11" font-family="ui-monospace, monospace">execute_workflow(...)</text>
        <line x1="190" y1="102" x2="190" y2="135" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="185,130 190,140 195,130" fill="#a0aec0"/>
        <rect x="20" y="145" width="340" height="200" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="168" text-anchor="middle" fill="#e2e8f0" font-size="13" font-weight="600">Temporal Server</text>
        <text x="190" y="190" text-anchor="middle" fill="#f6e05e" font-size="22" font-weight="700">11 events</text>
        <rect x="40" y="205" width="300" height="125" fill="#1a1a2e" stroke="#7350f7" stroke-width="1.2" rx="4"/>
        <text x="190" y="223" text-anchor="middle" fill="#b794f6" font-size="11" font-weight="600">Workflow Execution</text>
        <text x="55" y="244" fill="#cbd5e0" font-size="10" font-weight="600">1.</text>
        <text x="75" y="244" fill="#a0aec0" font-size="10">WorkflowExecutionStarted</text>
        <text x="55" y="262" fill="#cbd5e0" font-size="10" font-weight="600">2-4.</text>
        <text x="92" y="262" fill="#a0aec0" font-size="10">WorkflowTask Scheduled / Started / Completed</text>
        <text x="55" y="280" fill="#cbd5e0" font-size="10" font-weight="600">5-7.</text>
        <text x="92" y="280" fill="#a0aec0" font-size="10">ActivityTask Scheduled / Started / Completed</text>
        <text x="55" y="298" fill="#cbd5e0" font-size="10" font-weight="600">8-10.</text>
        <text x="96" y="298" fill="#a0aec0" font-size="10">WorkflowTask Scheduled / Started / Completed</text>
        <text x="55" y="316" fill="#cbd5e0" font-size="10" font-weight="600">11.</text>
        <text x="80" y="316" fill="#a0aec0" font-size="10">WorkflowExecutionCompleted</text>
        <line x1="190" y1="345" x2="190" y2="378" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="185,373 190,383 195,373" fill="#a0aec0"/>
        <rect x="110" y="388" width="160" height="54" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="411" text-anchor="middle" fill="#e2e8f0" font-size="13" font-weight="600">Worker</text>
        <text x="190" y="430" text-anchor="middle" fill="#a0aec0" font-size="11" font-family="ui-monospace, monospace">@workflow.defn + @activity.defn</text>
      </g>
      <text x="440" y="568" text-anchor="middle" fill="#cbd5e0" font-size="13">Same @activity.defn. Same HTTP delivery. About 3.7x the server-side events.</text>
    </svg>

    Click **Start** to spin up your sandbox.
tabs:
- id: cblvczchd9gl
  title: Editor
  type: code
  hostname: workshop
  path: /root/workshop
- id: u9ycx960dc7g
  title: Terminal
  type: terminal
  hostname: workshop
- id: uvg0hwykfsy3
  title: Worker
  type: terminal
  hostname: workshop
- id: sp8tl0pl7o8t
  title: Echo server
  type: service
  hostname: workshop
  port: 9000
- id: 7zrapmphtrzg
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 900
enhanced_loading: null
---

# Skip the workflow

Most Temporal tutorials show you Activities running inside Workflows. This module flips that: you'll write one Activity and run it **without** a Workflow at all — directly from a Temporal Client. Then you'll run the same Activity wrapped in a Workflow and see exactly what that wrapping costs you.

By the end you'll be able to:

- Invoke an Activity from a client with no Workflow class.
- Compare a Standalone Activity vs. an Activity-in-Workflow on **events, actions, retention, latency, and throughput**.
- Decide when the wrapping is worth it — and when it isn't.

Budget ~10 minutes.

---

## 1. Write the activity (~2 min)

Open `src/webhooks/activities.py` in the [button label="Editor" background="#444CE7"](tab-0) tab. You'll see a `deliver_webhook` function with three TODOs:

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
