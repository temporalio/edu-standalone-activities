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

    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1100 540" font-family="system-ui, -apple-system, 'Segoe UI', sans-serif">
      <rect width="1100" height="540" fill="#1a1a2e"/>
      <text x="550" y="30" text-anchor="middle" fill="#e2e8f0" font-size="20" font-weight="600">Standalone Activity vs. Activity-in-Workflow</text>
      <g transform="translate(20, 55)">
        <rect width="500" height="450" fill="none" stroke="#4a5568" stroke-dasharray="3 3" rx="6"/>
        <text x="250" y="22" text-anchor="middle" fill="#b794f6" font-size="14" font-weight="600">Standalone Activity</text>
        <rect x="170" y="38" width="160" height="38" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="250" y="58" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Client</text>
        <text x="250" y="71" text-anchor="middle" fill="#a0aec0" font-size="10" font-family="ui-monospace, monospace">execute_activity(...)</text>
        <line x1="250" y1="78" x2="250" y2="98" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="246,94 250,102 254,94" fill="#a0aec0"/>
        <rect x="20" y="105" width="460" height="200" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="250" y="125" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Temporal Server</text>
        <text x="250" y="155" text-anchor="middle" fill="#f6e05e" font-size="22" font-weight="700">3 events<animate attributeName="opacity" values="1;0.55;1" dur="2.4s" repeatCount="indefinite"/></text>
        <g font-size="11" font-family="ui-monospace, monospace">
          <circle cx="55" cy="205" r="3" fill="#7350f7"/>
          <text x="68" y="209" fill="#cbd5e0">1. ActivityTaskScheduled</text>
          <circle cx="105" cy="235" r="3" fill="#7350f7"/>
          <text x="118" y="239" fill="#cbd5e0">2. ActivityTaskStarted</text>
          <circle cx="155" cy="265" r="3" fill="#7350f7"/>
          <text x="168" y="269" fill="#cbd5e0">3. ActivityTaskCompleted</text>
        </g>
        <line x1="250" y1="309" x2="250" y2="335" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="246,331 250,339 254,331" fill="#a0aec0"/>
        <rect x="170" y="338" width="160" height="34" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="250" y="359" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Worker</text>
        <line x1="250" y1="376" x2="250" y2="402" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="246,398 250,406 254,398" fill="#a0aec0"/>
        <rect x="170" y="406" width="160" height="30" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="250" y="425" text-anchor="middle" fill="#e2e8f0" font-size="11">Echo Server</text>
        <circle r="6" fill="#f6e05e" stroke="#1a1a2e" stroke-width="1.5">
          <animate attributeName="cx" values="250;250;55;55;105;105;155;155;250;250;250;250" keyTimes="0;0.05;0.15;0.27;0.35;0.47;0.55;0.67;0.77;0.83;0.93;1" dur="7s" repeatCount="indefinite"/>
          <animate attributeName="cy" values="58;58;205;205;235;235;265;265;359;359;421;421" keyTimes="0;0.05;0.15;0.27;0.35;0.47;0.55;0.67;0.77;0.83;0.93;1" dur="7s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0;1;1;1;1;1;1;1;1;1;1;0" keyTimes="0;0.05;0.15;0.27;0.35;0.47;0.55;0.67;0.77;0.83;0.93;1" dur="7s" repeatCount="indefinite"/>
        </circle>
      </g>
      <g transform="translate(580, 55)">
        <rect width="500" height="450" fill="none" stroke="#4a5568" stroke-dasharray="3 3" rx="6"/>
        <text x="250" y="22" text-anchor="middle" fill="#b794f6" font-size="14" font-weight="600">Activity-in-Workflow</text>
        <rect x="170" y="38" width="160" height="38" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="250" y="58" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Client</text>
        <text x="250" y="71" text-anchor="middle" fill="#a0aec0" font-size="10" font-family="ui-monospace, monospace">execute_workflow(...)</text>
        <line x1="250" y1="78" x2="250" y2="98" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="246,94 250,102 254,94" fill="#a0aec0"/>
        <rect x="10" y="105" width="480" height="270" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="250" y="125" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Temporal Server</text>
        <text x="250" y="148" text-anchor="middle" fill="#f6e05e" font-size="22" font-weight="700">11 events<animate attributeName="opacity" values="1;0.55;1" dur="2.4s" repeatCount="indefinite"/></text>
        <rect x="25" y="160" width="450" height="210" fill="#1a1a2e" stroke="#7350f7" stroke-width="1.2" rx="4"/>
        <text x="250" y="177" text-anchor="middle" fill="#b794f6" font-size="11" font-weight="600">Workflow Execution</text>
        <g font-size="10" font-family="ui-monospace, monospace">
          <circle cx="40" cy="195" r="3" fill="#7350f7"/>
          <text x="52" y="199" fill="#cbd5e0">1. WorkflowExecutionStarted</text>
          <circle cx="55" cy="210" r="3" fill="#7350f7"/>
          <text x="67" y="214" fill="#cbd5e0">2. WorkflowTaskScheduled</text>
          <circle cx="70" cy="225" r="3" fill="#7350f7"/>
          <text x="82" y="229" fill="#cbd5e0">3. WorkflowTaskStarted</text>
          <circle cx="85" cy="240" r="3" fill="#7350f7"/>
          <text x="97" y="244" fill="#cbd5e0">4. WorkflowTaskCompleted</text>
          <circle cx="100" cy="255" r="3" fill="#7350f7"/>
          <text x="112" y="259" fill="#cbd5e0">5. ActivityTaskScheduled</text>
          <circle cx="115" cy="270" r="3" fill="#7350f7"/>
          <text x="127" y="274" fill="#cbd5e0">6. ActivityTaskStarted</text>
          <circle cx="130" cy="285" r="3" fill="#7350f7"/>
          <text x="142" y="289" fill="#cbd5e0">7. ActivityTaskCompleted</text>
          <circle cx="145" cy="300" r="3" fill="#7350f7"/>
          <text x="157" y="304" fill="#cbd5e0">8. WorkflowTaskScheduled</text>
          <circle cx="160" cy="315" r="3" fill="#7350f7"/>
          <text x="172" y="319" fill="#cbd5e0">9. WorkflowTaskStarted</text>
          <circle cx="175" cy="330" r="3" fill="#7350f7"/>
          <text x="187" y="334" fill="#cbd5e0">10. WorkflowTaskCompleted</text>
          <circle cx="190" cy="345" r="3" fill="#7350f7"/>
          <text x="202" y="349" fill="#cbd5e0">11. WorkflowExecutionCompleted</text>
        </g>
        <line x1="250" y1="379" x2="250" y2="402" stroke="#a0aec0" stroke-width="1.5"/>
        <polygon points="246,398 250,406 254,398" fill="#a0aec0"/>
        <rect x="170" y="406" width="160" height="34" fill="#2d3748" stroke="#4a5568" rx="4"/>
        <text x="250" y="425" text-anchor="middle" fill="#e2e8f0" font-size="12" font-weight="600">Worker</text>
        <text x="250" y="436" text-anchor="middle" fill="#a0aec0" font-size="9" font-family="ui-monospace, monospace">@workflow.defn + @activity.defn</text>
        <circle r="6" fill="#f6e05e" stroke="#1a1a2e" stroke-width="1.5">
          <animate attributeName="cx" values="250;250;40;55;70;85;100;115;130;145;160;175;190;250;250;250" keyTimes="0;0.03;0.08;0.15;0.22;0.29;0.36;0.43;0.50;0.57;0.64;0.71;0.78;0.86;0.95;1" dur="11s" repeatCount="indefinite"/>
          <animate attributeName="cy" values="58;58;195;210;225;240;255;270;285;300;315;330;345;425;425;425" keyTimes="0;0.03;0.08;0.15;0.22;0.29;0.36;0.43;0.50;0.57;0.64;0.71;0.78;0.86;0.95;1" dur="11s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0;1;1;1;1;1;1;1;1;1;1;1;1;1;1;0" keyTimes="0;0.03;0.08;0.15;0.22;0.29;0.36;0.43;0.50;0.57;0.64;0.71;0.78;0.86;0.95;1" dur="11s" repeatCount="indefinite"/>
        </circle>
      </g>
      <text x="550" y="530" text-anchor="middle" fill="#cbd5e0" font-size="12">Same @activity.defn. Same HTTP delivery. Up to 50% cheaper on Temporal Cloud.</text>
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
timelimit: 1500
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

In the [button label="Temporal UI" background="#444CE7"](tab-5) tab you will see a record of a completed Standalone Activity:

![Temporal UI showing a completed Standalone Activity in the Activities view](https://raw.githubusercontent.com/temporalio/edu-standalone-activities/impl/module-01/python/diagrams/standalone-activity-ui.png)

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

<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1100 620" font-family="system-ui, -apple-system, 'Segoe UI', sans-serif">
  <title>Standalone Activity vs Activity-in-Workflow — cost comparison</title>
  <rect width="1100" height="620" fill="#1a1a2e" rx="14"/>
  <g transform="translate(40, 32)">
    <circle cx="8" cy="10" r="7" fill="#7350f7"/>
    <text x="24" y="15" fill="#cbd5e0" font-size="12" font-weight="600" letter-spacing="2">TEMPORAL · STANDALONE ACTIVITIES</text>
  </g>
  <text x="550" y="90" text-anchor="middle" fill="#e2e8f0" font-size="28" font-weight="800">Same delivery. Different cost shape.</text>
  <text x="550" y="118" text-anchor="middle" fill="#a0aec0" font-size="14">Same @activity.defn. Same HTTP POST. Compared event-for-event.</text>
  <g transform="translate(0, 160)">
    <text x="380" y="0" text-anchor="middle" fill="#b794f6" font-size="14" font-weight="700" letter-spacing="3">STANDALONE</text>
    <text x="380" y="18" text-anchor="middle" fill="#7350f7" font-size="10" font-family="ui-monospace, monospace">client.execute_activity</text>
    <text x="820" y="0" text-anchor="middle" fill="#fc8181" font-size="14" font-weight="700" letter-spacing="3">IN A WORKFLOW</text>
    <text x="820" y="18" text-anchor="middle" fill="#f56565" font-size="10" font-family="ui-monospace, monospace">workflow → execute_activity</text>
  </g>
  <line x1="40" y1="200" x2="1060" y2="200" stroke="#4a5568" stroke-width="1" opacity="0.5"/>
  <line x1="600" y1="210" x2="600" y2="515" stroke="#4a5568" stroke-width="1" opacity="0.3" stroke-dasharray="2 4"/>
  <rect x="40" y="218" width="1020" height="64" fill="#252540" opacity="0.4" rx="8"/>
  <text x="60" y="256" fill="#cbd5e0" font-size="14" font-weight="600">Events / state transitions</text>
  <text x="380" y="265" text-anchor="middle" fill="#9ae6b4" font-size="42" font-weight="800">3</text>
  <text x="820" y="265" text-anchor="middle" fill="#fc8181" font-size="42" font-weight="800">11</text>
  <rect x="40" y="294" width="1020" height="50" fill="#252540" opacity="0.25" rx="6"/>
  <text x="60" y="324" fill="#cbd5e0" font-size="14" font-weight="600">Cloud actions billed*</text>
  <text x="380" y="330" text-anchor="middle" fill="#9ae6b4" font-size="28" font-weight="800">1</text>
  <text x="820" y="330" text-anchor="middle" fill="#fc8181" font-size="28" font-weight="800">≥ 2</text>
  <text x="60" y="378" fill="#a0aec0" font-size="13" font-weight="500">History retention</text>
  <text x="380" y="378" text-anchor="middle" fill="#e2e8f0" font-size="14">Activity-scoped</text>
  <text x="820" y="378" text-anchor="middle" fill="#e2e8f0" font-size="14">Workflow-scoped (full)</text>
  <text x="60" y="418" fill="#a0aec0" font-size="13" font-weight="500">Visibility</text>
  <text x="380" y="418" text-anchor="middle" fill="#cbd5e0" font-size="12" font-family="ui-monospace, monospace">temporal activity describe</text>
  <text x="820" y="418" text-anchor="middle" fill="#e2e8f0" font-size="14">Full timeline + search</text>
  <text x="60" y="458" fill="#a0aec0" font-size="13" font-weight="500">Latency overhead</text>
  <text x="380" y="458" text-anchor="middle" fill="#9ae6b4" font-size="14" font-weight="700">Lower ▼</text>
  <text x="820" y="458" text-anchor="middle" fill="#fc8181" font-size="14" font-weight="700">Higher ▲</text>
  <text x="60" y="498" fill="#a0aec0" font-size="13" font-weight="500">Throughput at scale</text>
  <text x="380" y="498" text-anchor="middle" fill="#9ae6b4" font-size="14" font-weight="700">Higher ▲</text>
  <text x="820" y="498" text-anchor="middle" fill="#fc8181" font-size="14" font-weight="700">Lower ▼</text>
  <rect x="180" y="530" width="740" height="60" fill="#2d3748" stroke="#7350f7" stroke-width="1.5" rx="10"/>
  <text x="550" y="558" text-anchor="middle" fill="#f6e05e" font-size="20" font-weight="800">Up to 50% cheaper on Temporal Cloud</text>
  <text x="550" y="578" text-anchor="middle" fill="#cbd5e0" font-size="11">Workflows give you orchestration. Standalone activities skip the scaffolding when you don't need it.</text>
</svg>

\* Approximate; check current Temporal Cloud pricing for the exact billing model.

Also open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and click into both executions. The workflow view has a full timeline with task events around the activity; the standalone view has just the activity itself.

> **What's happening:** Wrapping an Activity in a Workflow gives you orchestration — signals, queries, child workflows, multi-step state. If you don't need any of that, you're paying the wrapping cost (events, actions, retention, latency) for nothing. Standalone is the right shape for one-shot durable work.

---

## Check your understanding

> Your Workflow calls 5 Activities sequentially before returning. Roughly how many _more_ events does that emit compared to firing the same 5 Activities as Standalone Activities?

<details>
<summary>Answer</summary>

About **2×** more. A Workflow that calls 5 Activities emits roughly 30+ events (each Activity is bracketed by Workflow Task events, plus Workflow-start and Workflow-end). Five Standalone Activities emit ~15 events total (3 each).

Multiply by millions of executions per day and you have a real line item in retention storage and per-action billing.

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
