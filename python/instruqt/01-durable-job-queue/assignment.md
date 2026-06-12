---
slug: durable-job-queue
id: yuewx9heodgi
type: challenge
title: 'Standalone Activities: the durable job queue'
teaser: Run a webhook delivery as a durable job, with no broker, scheduler, or result
  store to operate.
notes:
- type: text
  contents: |
    # Standalone Activities in Python

    *By [Nikolay Advolodkin](https://www.linkedin.com/in/nikolayadvolodkin/), Staff Developer Advocate at Temporal*

    You're going to build a durable webhook delivery service.

    When something happens in your application, such as a payment clearing, an order shipping, or a user signing up, you POST to a URL another team gave you. Doing it durably means: if the network fails, retry. If the receiver returns 500, retry. If your service crashes mid-send, the retry does not double-deliver.

    The same deliver_webhook Activity runs through every module of this tutorial:

    - **Module 1**: Run the Activity directly from a client. Inspect it in the Temporal UI.
    - **Module 2**: Make retries safe with an idempotency key.
    - **Module 3**: Reject duplicate jobs at the platform level.
    - **Module 4**: Cap throughput and prioritize urgent jobs.
    - **Module 5**: Long-running jobs that heartbeat progress and resume after a crash.
    - **Module 6**: The same Activity, called from a Workflow. Same code, two job types.

    ## What's already running in this sandbox

    - **Temporal Service and Web UI**: already running and ready for the exercises.
    - **Webhook receiver**: records webhook deliveries so you can verify what left your Worker actually landed.

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
        <text x="140" y="166" text-anchor="middle" fill="#a0aec0" font-size="12">has the answer. Peek any time</text>
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

# Submit a durable job with one API call

With many job queues, you still have to solve the hard parts yourself:

- Jobs vanish during deploys, crashes, and restarts.
- Bring your own broker, result store, scheduler, and monitoring.
- Retry logic reimplemented in every service, all behaving differently.
- Slow consumers can block everything behind them.
- If the work grows into orchestration, you often have to rewrite it elsewhere.
- No polyglot support in most job queue frameworks.
- A Tier-0 service nobody wants to maintain.

**Standalone Activities are Temporal's durable job queue.** You write a regular `@activity.defn` and submit it with one API call. Temporal persists it, retries it on failure, and makes it visible in the UI. You do not have to run a broker, scheduler, or result store.

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
2. `raise_for_status()` raises an exception if the response was a 4xx or 5xx. Temporal will see that and retry.
3. Return the HTTP status code as the Activity's result.

Instruqt auto-saves your edits. The full version is in the **Solution** tab if you'd rather copy it.

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

Open `src/webhooks/send_standalone.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. This call submits the durable job:

```python
await client.execute_activity(
    deliver_webhook,
    args=[WebhookDelivery(...)],
    id="deliver-evt_001",
    task_queue=TASK_QUEUE,
    start_to_close_timeout=timedelta(seconds=10),
)
```

One API call. The client tells Temporal, "run this Activity once and give me the result." There is no Workflow class in the script, and there is no broker, scheduler, or result store for you to deploy. Temporal persisted the job before acknowledging it, dispatched it to your Worker, and stored the result.

Open the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see one delivery recorded. The Webhook receiver tab auto-refreshes every 2 seconds, so leave it open and you'll see new deliveries appear without reloading.

---

## 3. Inspect the job in the Temporal UI (~2 min)

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and switch to the **Standalone Activities** tab in the left nav. You should see `deliver-evt_001` listed:

![Temporal UI showing a completed Standalone Activity in the Standalone Activities tab](https://raw.githubusercontent.com/temporalio/edu-standalone-activities/standalone-pre/python/diagrams/standalone-activity-ui.png)

Click into it. Temporal is now handling three things you would otherwise have to build or operate:

- **Addressable.** Every job has a stable ID (`deliver-evt_001` here). You can query its status, fetch its result, cancel it, or terminate it from the UI, the CLI, the SDK, or the API.
- **Durable.** The job was persisted before your Worker even saw it. If the Worker had crashed mid-delivery, the same job would have been redispatched to a Worker polling the Task Queue on retry.
- **Observable.** Status, attempt count, the last error, and the result are visible without a separate logging pipeline.

That is the shape we want from a job queue: durable execution, retries, status, and results without extra infrastructure to operate.

---

## Check your understanding

> Your `deliver_webhook` job hits a transient 503 from the receiver on attempt 1. With Temporal's default RetryPolicy, what happens?

<details>
<summary>Answer</summary>

Temporal sees the exception, waits the initial retry interval (1s by default), and dispatches the job again. The retry policy is exponential, so later failures wait longer before retrying. You did not write retry code; you configured it on the Activity options. The job stays "Running" in the UI through every retry, and you can watch the attempt counter increment.

Contrast: in a traditional job queue, retry behavior is something you re-implement per service, with subtle differences each time.

</details>

## Coming up

The next modules tackle what happens when reality intrudes:

- **Module 02**: Idempotency and crash safety. Crash the Worker mid-delivery, watch the receiver show duplicates, then fix it.
- **Module 03**: Deduplication via ID reuse. Submit the same job ID twice and let Temporal return the existing handle.
- **Module 04**: Concurrency, rate limits, priority and fairness. Stop one busy tenant from slowing everyone else down.
- **Module 05**: Heartbeats and checkpointing. Resume long-running jobs from the last reported progress after a crash.
- **Module 06**: Same code runs anywhere. Call the same `deliver_webhook` Activity from a Workflow.

---

[**Share your feedback**](https://forms.gle/Tvx6YenX7zTHgrqU8)
