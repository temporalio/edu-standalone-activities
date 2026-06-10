---
slug: skip-the-workflow
id: yuewx9heodgi
type: challenge
title: Skip the workflow
teaser: Build a webhook delivery activity, run it standalone, run it via a workflow,
  and compare the cost.
notes:
- type: text
  contents: |
    # Standalone Activities in Python

    You're going to build a durable webhook delivery service.

    When something happens in your application — a payment clears, an order ships, a user signs up — you often need to tell another system about it by POSTing to a URL the other team gave you. That POST is called a **webhook**. Doing it durably means: if the network fails, retry. If the receiver returns 500, retry. If your service crashes mid-send, the retry doesn't double-deliver.

    The same `deliver_webhook` Activity runs through every module:

    - **Module 1**: Run the Activity directly from a client, with no Workflow. Compare the server-side cost vs. running the same Activity inside a Workflow.
    - **Module 2**: Make retries safe with an idempotency key.
    - **Module 3**: Cap throughput so a burst of deliveries doesn't overload the receiver.
    - **Module 4**: Reject duplicate requests at Temporal's scheduling layer.
    - **Module 5**: Three real-world scenarios — pick Standalone Activity or Workflow for each.

    ## What's already running in this sandbox

    - **Temporal dev server** on `localhost:7233` (single-binary dev mode).
    - **Temporal Web UI** on `localhost:8233` — browse activities and workflows.
    - **Webhook receiver** on `localhost:9000` — a tiny HTTP server that records every webhook it receives. You'll use it to verify the deliveries that left your Worker actually landed.

    You don't need to start any of these. They boot with the sandbox.

    ## Prerequisites

    - Comfortable reading and writing Python (functions, classes, imports).
    - Familiar with Temporal Activities and Workers at the level [Temporal 101 in Python](https://learn.temporal.io/courses/temporal_101/python/) covers. If those words are new, take that course first and come back.

    ## How this tutorial works

    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 880 220" font-family="system-ui, -apple-system, 'Segoe UI', sans-serif">
      <g transform="translate(0, 0)">
        <rect width="280" height="220" fill="#252540" stroke="#3a3158" stroke-width="1" rx="10"/>
        <rect x="60" y="40" width="160" height="44" fill="#444CE7" rx="6"/>
        <text x="140" y="68" text-anchor="middle" fill="#fff" font-size="15" font-weight="600">Worker</text>
        <text x="140" y="142" text-anchor="middle" fill="#e2e8f0" font-size="14" font-weight="600">Click blue buttons</text>
        <text x="140" y="166" text-anchor="middle" fill="#a0aec0" font-size="12">to jump to that tab</text>
      </g>
      <g transform="translate(300, 0)">
        <rect width="280" height="220" fill="#252540" stroke="#3a3158" stroke-width="1" rx="10"/>
        <rect x="20" y="35" width="240" height="58" fill="#1a1a2e" stroke="#4a5568" rx="4"/>
        <text x="35" y="58" fill="#7aa2ff" font-size="11" font-family="ui-monospace, monospace">$ uv run python</text>
        <text x="35" y="76" fill="#9ae6b4" font-size="11" font-family="ui-monospace, monospace">    -m webhooks.worker</text>
        <rect x="195" y="44" width="55" height="28" fill="#444CE7" rx="4"/>
        <text x="222" y="62" text-anchor="middle" fill="#fff" font-size="11" font-weight="600">▶ Run</text>
        <text x="140" y="142" text-anchor="middle" fill="#e2e8f0" font-size="14" font-weight="600">Click the Run button</text>
        <text x="140" y="166" text-anchor="middle" fill="#a0aec0" font-size="12">to execute in a terminal</text>
      </g>
      <g transform="translate(600, 0)">
        <rect width="280" height="220" fill="#252540" stroke="#3a3158" stroke-width="1" rx="10"/>
        <rect x="20" y="42" width="60" height="32" fill="#2d3748" stroke="#4a5568" rx="3"/>
        <text x="50" y="63" text-anchor="middle" fill="#a0aec0" font-size="10">Exercise</text>
        <rect x="88" y="42" width="60" height="32" fill="#9ae6b4" rx="3"/>
        <text x="118" y="63" text-anchor="middle" fill="#1a1a2e" font-size="10" font-weight="700">Solution</text>
        <rect x="156" y="42" width="60" height="32" fill="#2d3748" stroke="#4a5568" rx="3"/>
        <text x="186" y="63" text-anchor="middle" fill="#a0aec0" font-size="10">Terminal</text>
        <rect x="224" y="42" width="36" height="32" fill="#2d3748" stroke="#4a5568" rx="3"/>
        <text x="242" y="63" text-anchor="middle" fill="#a0aec0" font-size="10">...</text>
        <text x="140" y="142" text-anchor="middle" fill="#e2e8f0" font-size="14" font-weight="600">Solution tab</text>
        <text x="140" y="166" text-anchor="middle" fill="#a0aec0" font-size="12">has the answer — peek any time</text>
      </g>
    </svg>
tabs:
- id: vryel8ovmbex
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/01-skip-the-workflow/exercise
- id: y9g12yc9sk4i
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/01-skip-the-workflow/solution
- id: xyqbqlaekkix
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-skip-the-workflow/exercise
- id: lyxq5rizt6bi
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-skip-the-workflow/exercise
- id: sgg6rcekwwk2
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: x7aermetxpnt
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Run an Activity without a Workflow

In Temporal you usually run an Activity from inside a Workflow. In this module you'll do something different: you'll run the same Activity directly from a client, with no Workflow involved. Temporal calls this a **Standalone Activity**.

It's still durable. It still retries on failure. It still shows up in the Temporal UI. But because there's no Workflow Execution wrapping it, Temporal records fewer events, retains less history, and bills fewer actions. For one-shot work like sending a webhook, charging a card, or sending an email — where you don't need multi-step orchestration — that overhead is wasted. Standalone Activities let you skip it.

You'll do four things in this module:

1. Write a small `deliver_webhook` Activity in Python.
2. Run it directly from a client (Standalone Activity).
3. Run the same Activity wrapped in a tiny Workflow.
4. Compare how many events Temporal recorded for each, using the CLI.

Estimated time: 10 minutes.

---

## 1. Write the Activity (~2 min)

Open `src/webhooks/activities.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. You'll see a stub with three `TODO` comments and a `raise NotImplementedError`.

Replace the body of `deliver_webhook` with this code:

```python
response = httpx.post(req.url, json=req.payload, timeout=5.0)
response.raise_for_status()
return response.status_code
```

Three lines:

1. POST the payload to the URL using `httpx`. Both come from the `WebhookDelivery` input. `httpx` is already installed.
2. `raise_for_status()` raises an exception if the response was a 4xx or 5xx — Temporal will see that and retry.
3. Return the HTTP status code as the Activity's result.

Save the file. The full version is in the **Solution** tab if you'd rather copy it.

> This is a regular `@activity.defn`. There's no "standalone" decorator. Standalone vs. workflow-bound is decided by *how the Activity is called*, not how it's defined.

---

## 2. Run it as a Standalone Activity (~2 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
uv run python -m webhooks.worker
```

You should see:

```
Worker running on task queue 'webhook-queue'
```

The Worker is now polling Temporal for tasks. Leave it running.

In the [button label="Terminal" background="#444CE7"](tab-2) tab, run the starter script:

```bash,run
uv run python -m webhooks.send_standalone evt_001
```

You should see:

```
Standalone activity completed with status 200
```

Open `src/webhooks/send_standalone.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. The interesting line is:

```python
await client.execute_activity(
    deliver_webhook,
    args=[WebhookDelivery(...)],
    id="deliver-evt_001",
    task_queue=TASK_QUEUE,
    start_to_close_timeout=timedelta(seconds=10),
)
```

The client tells Temporal "run this Activity once and give me the result." There's no Workflow class anywhere in the script.

Open the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see one delivery recorded. The Webhook receiver tab auto-refreshes every 2 seconds, so leave it open and you'll see new deliveries appear without reloading.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and switch to the **Standalone Activities** tab in the left nav. You should see `deliver-evt_001` listed:

![Temporal UI showing a completed Standalone Activity in the Standalone Activities tab](https://raw.githubusercontent.com/temporalio/edu-standalone-activities/standalone-pre/python/diagrams/standalone-activity-ui.png)

---

## 3. Run the same Activity inside a Workflow (~2 min)

`src/webhooks/workflow.py` defines a tiny Workflow that just wraps `deliver_webhook`:

```python
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

You don't need to edit anything. Just read it, then in the [button label="Terminal" background="#444CE7"](tab-2) tab run the second starter:

```bash,run
uv run python -m webhooks.send_via_workflow evt_002
```

You should see:

```
Workflow completed with activity returning status 200
```

Open `src/webhooks/send_via_workflow.py` and compare it to `send_standalone.py`:

- `send_standalone.py` calls `client.execute_activity(deliver_webhook, ...)` — Temporal runs your Activity directly.
- `send_via_workflow.py` calls `client.execute_workflow(WebhookWorkflow.run, ...)` — Temporal runs a Workflow Execution, which then runs your Activity.

Same `deliver_webhook` Activity gets called both times. The Webhook receiver tab now shows **2** deliveries.

---

## 4. Compare the cost (~3 min)

From the outside, the two runs look identical: one HTTP POST landed at the Webhook receiver each time. But Temporal recorded very different amounts of history for each. Look at both with the CLI in the [button label="Terminal" background="#444CE7"](tab-2) tab:

```bash,run
temporal activity describe --address localhost:7233 --activity-id deliver-evt_001
```

Near the bottom of the output, look for `StateTransitionCount`:

```
  ActivityId               deliver-evt_001
  Type                     deliver_webhook
  Status                   Completed
  Attempt                  1
  StateTransitionCount     3
```

**3** state transitions for the Standalone Activity: scheduled, started, completed. Now the Workflow version:

```bash,run
temporal workflow show --address localhost:7233 --workflow-id wf-evt_002
```

Output:

```
Progress:
  ID           Type
    1  WorkflowExecutionStarted
    2  WorkflowTaskScheduled
    3  WorkflowTaskStarted
    4  WorkflowTaskCompleted
    5  ActivityTaskScheduled
    6  ActivityTaskStarted
    7  ActivityTaskCompleted
    8  WorkflowTaskScheduled
    9  WorkflowTaskStarted
   10  WorkflowTaskCompleted
   11  WorkflowExecutionCompleted
```

**11** events for the Workflow run. The Activity itself took 3 events (5, 6, 7). The other 8 events are Workflow bookkeeping — start, end, and the Workflow Task events that bracket the Activity.

At low volume that's nothing. At 10 million webhook deliveries a day:

- 30 million events vs. 110 million events. 80 million fewer events recorded.
- Roughly half the actions billed on Temporal Cloud.
- Less event history retained per delivery.

You also lose some things by going standalone:

| You get | You give up |
| --- | --- |
| Fewer events, less retention, fewer billed actions | Multi-step orchestration |
| Lower latency overhead | Signals, queries, child workflows |
| Higher throughput per Worker | Workflow-level compensation / saga semantics |
| Activity still shows in the Temporal UI | Full timeline view (you get the Activity record, not a Workflow history) |

If your delivery is a single self-contained POST, the Workflow scaffolding is paying for features you're not using. If you ever need to coordinate multiple steps (charge the card, then reserve inventory, then send the email, with compensation if any step fails), you want a Workflow.

---

## Check your understanding

> Your Workflow calls 5 Activities sequentially before returning. Roughly how many _more_ events does that emit compared to running the same 5 Activities as Standalone Activities?

<details>
<summary>Answer</summary>

About **2×** more. A Workflow that calls 5 Activities emits roughly 30+ events (each Activity is bracketed by Workflow Task events, plus Workflow-start and Workflow-end). Five Standalone Activities emit ~15 events total (3 each).

Multiply by millions of executions per day and you have a real line item in retention storage and per-action billing.

</details>

## Coming up

The next modules tackle what happens when reality intrudes:

- **Module 02** — Idempotency and crash safety. Crash the worker mid-delivery; watch the webhook receiver show 2 deliveries; fix it.
- **Module 03** — Concurrency, rate limits, priority & fairness. Stop one loud tenant from starving the rest.
- **Module 04** — Dedup via ID reuse. Same upstream event arrives twice; let Temporal reject the duplicate.
- **Module 05** — When SAA vs. when Workflow. Three scenarios, your call.
