---
slug: durable-job-queue
id: yuewx9heodgi
type: challenge
title: Standalone Activities — the durable job queue
teaser: Run a webhook delivery as a durable job — no broker, no scheduler, no result
  store to operate.
notes:
- type: text
  contents: |
    # Standalone Activities in Python

    You're going to build a durable webhook delivery service.

    When something happens in your application — a payment clears, an order ships, a user signs up — you often need to tell another system about it by POSTing to a URL the other team gave you. That POST is called a **webhook**. Doing it durably means: if the network fails, retry. If the receiver returns 500, retry. If your service crashes mid-send, the retry doesn't double-deliver.

    The traditional way to handle background work like this is a job queue — and it leaves you holding the bag:

    - Jobs vanish during deploys, crashes, and restarts.
    - Bring your own broker, result store, scheduler, and monitoring.
    - Retry logic is reimplemented in every service, all behaving differently.
    - No upgrade path when you outgrow it.
    - It's a Tier-0 service nobody wants to maintain.

    **Standalone Activities are Temporal's durable job queue.** Same `@activity.defn` you already know — submitted as a top-level job, durably persisted, retried on failure, addressable in the UI. No broker, no scheduler, no result store to operate. And the same Activity code runs inside a Workflow later when the job grows into a multi-step process.

    The same `deliver_webhook` Activity runs through every module of this tutorial:

    - **Module 1**: Run the Activity directly from a client. Inspect it in the Temporal UI.
    - **Module 2**: Make retries safe with an idempotency key.
    - **Module 3**: Reject duplicate jobs at the platform level.
    - **Module 4**: Cap throughput and prioritize urgent jobs.
    - **Module 5**: Long-running jobs that heartbeat progress and resume after a crash.
    - **Module 6**: The same Activity, called from a Workflow. Same code, two job types.

    ## What's already running in this sandbox

    - **Temporal dev server** on `localhost:7233` (single-binary dev mode).
    - **Temporal Web UI** on `localhost:8233` — browse Activities and Workflows.
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
  path: /root/workshop/exercises/01-durable-job-queue/exercise
- id: y9g12yc9sk4i
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/01-durable-job-queue/solution
- id: xyqbqlaekkix
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-durable-job-queue/exercise
- id: lyxq5rizt6bi
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-durable-job-queue/exercise
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

# Submit a durable job — no Workflow required

In Temporal you usually run an Activity from inside a Workflow. In this module you'll do something different: you'll run the same Activity directly from a client, with no Workflow involved. Temporal calls this a **Standalone Activity** — and it's the durable job queue at the heart of this tutorial.

It's still durable. It still retries on failure. It's addressable in the Temporal UI. You can submit it from any of five SDKs. And the Activity code itself is unchanged — same `@activity.defn` you'd use inside a Workflow. The only thing different is *how it's called*.

You'll do three things in this module:

1. Write a small `deliver_webhook` Activity in Python.
2. Submit it as a Standalone Activity from a client.
3. Inspect the running job in the Temporal UI.

Estimated time: 7 minutes.

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

> This is a regular `@activity.defn`. There's no "standalone" decorator. Standalone vs. inside-a-Workflow is decided by *how the Activity is called*, not how it's defined.

---

## 2. Submit it as a Standalone Activity (~3 min)

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
Standalone Activity completed with status 200
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

One API call. The client tells Temporal "run this Activity once and give me the result." There's no Workflow class anywhere in the script — and there's no broker, scheduler, or result store you had to deploy either. Temporal persisted the job to durable storage before acknowledging it, dispatched it to your Worker, and stored the result for you.

Open the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see one delivery recorded. The Webhook receiver tab auto-refreshes every 2 seconds, so leave it open and you'll see new deliveries appear without reloading.

---

## 3. Inspect the job in the Temporal UI (~2 min)

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and switch to the **Standalone Activities** tab in the left nav. You should see `deliver-evt_001` listed:

![Temporal UI showing a completed Standalone Activity in the Standalone Activities tab](https://raw.githubusercontent.com/temporalio/edu-standalone-activities/standalone-pre/python/diagrams/standalone-activity-ui.png)

Click into it. Three things you get for free from the platform — without operating any of them yourself:

- **Addressable.** Every job has a stable ID (`deliver-evt_001` here). You can query its status, fetch its result, terminate it, or delete it — from the UI, the CLI, the SDK, or the API.
- **Durable.** The job was persisted before your Worker even saw it. If the Worker had crashed mid-delivery, the same job would have been redispatched to another Worker on retry.
- **Observable.** Failed attempts, retry timing, payload, result — all there. No separate logging pipeline.

This is what your job queue should have been doing all along. It's running on Temporal, not on infrastructure you built and operate.

---

## Check your understanding

> Your `deliver_webhook` job hits a transient 503 from the receiver on attempt 1. With Temporal's default RetryPolicy, what happens?

<details>
<summary>Answer</summary>

Temporal sees the exception, waits the initial retry interval (1s by default), and dispatches the job again. The retry policy is exponential: subsequent failures back off further. You didn't write any retry code — it's declarative on the Activity options. The job stays "Running" in the UI through every retry; you can watch the attempt counter increment.

Contrast: in a traditional job queue, retry behavior is something you re-implement per service, with subtle differences each time.

</details>

## Coming up

The next modules tackle what happens when reality intrudes:

- **Module 02** — Idempotency and crash safety. Crash the Worker mid-delivery; watch the receiver show duplicates; fix it.
- **Module 03** — Deduplication via ID reuse. Same job ID submitted twice; let Temporal return the existing handle.
- **Module 04** — Concurrency, rate limits, priority and fairness. Stop one loud tenant from starving the rest.
- **Module 05** — Heartbeats and checkpointing. Long-running jobs that resume from the last reported progress after a crash.
- **Module 06** — Same code runs anywhere. The same `deliver_webhook` Activity, now called from a Workflow.
