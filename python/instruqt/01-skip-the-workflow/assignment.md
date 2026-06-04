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

    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 880 420" font-family="system-ui, -apple-system, 'Segoe UI', sans-serif">
      <rect width="880" height="420" fill="#1a1a2e"/>
      <text x="440" y="28" text-anchor="middle" fill="#e2e8f0" font-size="18" font-weight="600">Standalone Activity vs. Activity-in-Workflow</text>
      <g transform="translate(40, 50)">
        <rect width="380" height="340" fill="none" stroke="#4a5568" stroke-dasharray="3 3" rx="6"/>
        <text x="190" y="22" text-anchor="middle" fill="#b794f6" font-size="14" font-weight="600">Standalone Activity</text>
        <rect x="110" y="38" width="160" height="40" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="58" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Client</text>
        <text x="190" y="73" text-anchor="middle" fill="#a0aec0" font-size="10" font-family="ui-monospace, monospace">execute_activity(...)</text>
        <line x1="190" y1="80" x2="190" y2="100" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="186,96 190,104 194,96" fill="#a0aec0"/>
        <rect x="50" y="105" width="280" height="110" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="125" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Temporal Server</text>
        <text x="190" y="152" text-anchor="middle" fill="#f6e05e" font-size="20" font-weight="700">3 events<animate attributeName="opacity" values="1;0.55;1" dur="2.4s" repeatCount="indefinite"/></text>
        <text x="190" y="175" text-anchor="middle" fill="#a0aec0" font-size="10">ActivityTaskScheduled</text>
        <text x="190" y="190" text-anchor="middle" fill="#a0aec0" font-size="10">ActivityTaskStarted</text>
        <text x="190" y="205" text-anchor="middle" fill="#a0aec0" font-size="10">ActivityTaskCompleted</text>
        <line x1="190" y1="217" x2="190" y2="237" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="186,233 190,241 194,233" fill="#a0aec0"/>
        <rect x="110" y="242" width="160" height="40" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="259" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Worker</text>
        <text x="190" y="274" text-anchor="middle" fill="#a0aec0" font-size="10" font-family="ui-monospace, monospace">@activity.defn</text>
        <line x1="190" y1="284" x2="190" y2="304" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="186,300 190,308 194,300" fill="#a0aec0"/>
        <rect x="110" y="308" width="160" height="28" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="326" text-anchor="middle" fill="#e2e8f0" font-size="11">Echo Server</text>
        <circle r="4.5" fill="#b794f6">
          <animate attributeName="cx" values="190" dur="3s" repeatCount="indefinite"/>
          <animate attributeName="cy" values="58;58;160;260;320" keyTimes="0;0.08;0.4;0.7;1" dur="3s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0;1;1;1;0" keyTimes="0;0.08;0.5;0.9;1" dur="3s" repeatCount="indefinite"/>
        </circle>
      </g>
      <g transform="translate(460, 50)">
        <rect width="380" height="340" fill="none" stroke="#4a5568" stroke-dasharray="3 3" rx="6"/>
        <text x="190" y="22" text-anchor="middle" fill="#b794f6" font-size="14" font-weight="600">Activity-in-Workflow</text>
        <rect x="110" y="38" width="160" height="40" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="58" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Client</text>
        <text x="190" y="73" text-anchor="middle" fill="#a0aec0" font-size="10" font-family="ui-monospace, monospace">execute_workflow(...)</text>
        <line x1="190" y1="80" x2="190" y2="100" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="186,96 190,104 194,96" fill="#a0aec0"/>
        <rect x="20" y="105" width="340" height="160" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="125" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Temporal Server</text>
        <text x="190" y="147" text-anchor="middle" fill="#f6e05e" font-size="20" font-weight="700">11 events<animate attributeName="opacity" values="1;0.55;1" dur="2.4s" repeatCount="indefinite"/></text>
        <rect x="40" y="158" width="300" height="100" fill="#1a1a2e" stroke="#7350f7" stroke-width="1.2" rx="4"/>
        <text x="190" y="174" text-anchor="middle" fill="#b794f6" font-size="11" font-weight="600">Workflow Execution</text>
        <text x="55" y="192" fill="#cbd5e0" font-size="9" font-weight="600">1.</text>
        <text x="75" y="192" fill="#a0aec0" font-size="9">WorkflowExecutionStarted</text>
        <text x="55" y="207" fill="#cbd5e0" font-size="9" font-weight="600">2-4.</text>
        <text x="92" y="207" fill="#a0aec0" font-size="9">WorkflowTask Scheduled / Started / Completed</text>
        <text x="55" y="222" fill="#cbd5e0" font-size="9" font-weight="600">5-7.</text>
        <text x="92" y="222" fill="#a0aec0" font-size="9">ActivityTask Scheduled / Started / Completed</text>
        <text x="55" y="237" fill="#cbd5e0" font-size="9" font-weight="600">8-10.</text>
        <text x="96" y="237" fill="#a0aec0" font-size="9">WorkflowTask Scheduled / Started / Completed</text>
        <text x="55" y="252" fill="#cbd5e0" font-size="9" font-weight="600">11.</text>
        <text x="80" y="252" fill="#a0aec0" font-size="9">WorkflowExecutionCompleted</text>
        <line x1="190" y1="267" x2="190" y2="287" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="186,283 190,291 194,283" fill="#a0aec0"/>
        <rect x="110" y="292" width="160" height="40" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="190" y="309" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Worker</text>
        <text x="190" y="324" text-anchor="middle" fill="#a0aec0" font-size="10" font-family="ui-monospace, monospace">@workflow + @activity</text>
        <circle r="4.5" fill="#b794f6">
          <animate attributeName="cx" values="190" dur="3s" repeatCount="indefinite"/>
          <animate attributeName="cy" values="58;58;180;210;240;305;310" keyTimes="0;0.06;0.25;0.45;0.65;0.92;1" dur="3s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0;1;1;1;1;1;0" keyTimes="0;0.06;0.25;0.45;0.65;0.92;1" dur="3s" repeatCount="indefinite"/>
        </circle>
      </g>
      <text x="440" y="410" text-anchor="middle" fill="#cbd5e0" font-size="12">Same @activity.defn. Same HTTP delivery. Up to 50% cheaper on Temporal Cloud.</text>
    </svg>
tabs:
- id: cblvczchd9gl
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/01-skip-the-workflow/exercise
- id: xhxihbdamu3g
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/01-skip-the-workflow/solution
- id: u9ycx960dc7g
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-skip-the-workflow/exercise
- id: uvg0hwykfsy3
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-skip-the-workflow/exercise
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

Open `src/webhooks/activities.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. You'll see a `deliver_webhook` function with three TODOs:

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

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the worker:

```bash,run
uv run python -m webhooks.worker
```

Expected:

```
Worker running on task queue 'webhook-queue'
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, fire one delivery as a Standalone Activity:

```bash,run
uv run python -m webhooks.send_standalone evt_001
```

Expected:

```
Standalone activity completed with status 200
```

Open the [button label="Echo server" background="#444CE7"](tab-4) tab. You'll see one delivery in the JSON.

> **What's happening:** Look at `send_standalone.py`. The whole call is `await client.execute_activity(deliver_webhook, ...)`. **No `@workflow.defn` anywhere in your code.** The client tells Temporal "schedule this activity"; Temporal hands it to your worker; the result comes back. It's a typed durable job queue.

---

## 3. Run the same activity inside a workflow (~2 min)

A 5-line `WebhookWorkflow` is provided in `src/webhooks/workflow.py`. You don't need to edit it — it just wraps `deliver_webhook` and calls it via `workflow.execute_activity`. Open it and read it once so you see the shape.

In the [button label="Terminal" background="#444CE7"](tab-2) tab, fire one delivery through the workflow:

```bash,run
uv run python -m webhooks.send_via_workflow evt_002
```

Expected:

```
Workflow completed with activity returning status 200
```

Refresh the [button label="Echo server" background="#444CE7"](tab-4) tab. You should now see **2** deliveries total — one per call.

> **What's happening:** Same Activity. Same business outcome. But the second one was scheduled inside a Workflow execution. Both are durable. Both are retried on failure (no failures today). Both show up in the Temporal UI. The difference shows up in how much Temporal had to record to make that happen.

---

## 4. Compare the cost (~3 min)

The two ways look identical from the outside — but Temporal did very different amounts of work under the hood. Run these side by side in the [button label="Terminal" background="#444CE7"](tab-2) tab:

```bash,run
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

Also open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and click into both executions. The workflow view has a full timeline with task events around the activity; the standalone view has just the activity itself.

> **What's happening:** Wrapping an Activity in a Workflow gives you orchestration — signals, queries, child workflows, multi-step state. If you don't need any of that, you're paying the wrapping cost (events, actions, retention, latency) for nothing. Standalone is the right shape for one-shot durable work.

---

## Check your understanding

> Your Workflow calls 5 Activities sequentially before returning. Roughly how many *more* events does that emit compared to firing the same 5 Activities as Standalone Activities?

<details>
<summary>Answer</summary>

About **2×** more. A Workflow that calls 5 Activities emits roughly 30+ events (each Activity is bracketed by Workflow Task events, plus Workflow-start and Workflow-end). Five Standalone Activities emit ~15 events total (3 each).

Multiply by millions of executions per day and you have a real line item in retention storage and per-action billing — which is exactly why Stripe, Coinbase, and Rippling asked for Standalone Activities in the first place.

</details>

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
- **Module 05** — When SAA vs. when Workflow. Three scenarios, your call.
