---
slug: durable-job-queue
id: swvtvlqp5oty
type: challenge
title: 'Standalone Activities: the durable job queue'
teaser: Run a webhook delivery as a durable job, with no broker, scheduler, or result
  store to operate.
notes:
- type: text
  contents: |
    # Build a Job Queue with Standalone Activities (Tutorial)

    *By [Nikolay Advolodkin](https://www.linkedin.com/in/nikolayadvolodkin/), Staff Developer Advocate at Temporal*

    You're going to build a durable webhook delivery service.

    When something happens in your application - a payment clears, an order ships, a user signs up - you POST to a URL another team gave you. Doing it durably means: if the network fails, retry. If the receiver returns 500, retry. If your service crashes mid-send, the retry does not double-deliver.

    The same `deliverWebhook` activity runs through every module of this tutorial:

    - **Module 1**: Run the Activity directly from a client. Inspect it in the Temporal UI.
    - **Module 2**: Make retries safe with an idempotency key.
    - **Module 3**: Reject duplicate jobs at the platform level.
    - **Module 4**: Cap throughput and prioritize urgent jobs.
    - **Module 5**: Long-running jobs that heartbeat progress and resume after a crash.
    - **Module 6**: The same Activity, called from a Workflow. Same code, two job types.

    ## What's already running in this sandbox

    - **Temporal Service and Web UI**: already running and ready.
    - **Webhook receiver**: records webhook deliveries so you can verify what your Worker actually sent.

    You don't need to start any of these. They boot with the sandbox.

    ## Prerequisites

    - Comfortable reading and writing TypeScript (functions, interfaces, imports, async/await).
    - Familiar with Temporal Activities and Workers at the level [Temporal 101 in TypeScript](https://learn.temporal.io/courses/temporal_101/typescript/) covers. If those words are new, take that course first and come back.
tabs:
- id: se53gi9xr1js
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/01-durable-job-queue/exercise
- id: 3kngloyldrsm
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/01-durable-job-queue/solution
- id: wdo2nsxguhwl
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-durable-job-queue/exercise
- id: ykvdiyxo0av2
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/01-durable-job-queue/exercise
- id: xzf16klqcekk
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: bdifqx83wofe
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

**Standalone Activities are Temporal's durable job queue.** You write a regular TypeScript function and submit it with one API call. Temporal persists it, retries it on failure, and makes it visible in the UI. No broker, scheduler, or result store to deploy.

You'll do three things in this module:

1. Write a small `deliverWebhook` Activity in TypeScript.
2. Submit it as a Standalone Activity from a client.
3. Inspect the running job in the Temporal UI.

Estimated time: 7 minutes.

---

## 1. Write the Activity (~2 min)

Open `src/activities.ts` in the [button label="Exercise" background="#444CE7"](tab-0) tab. You'll see a stub with three `TODO` comments and a `throw new Error`.

Replace the body of `deliverWebhook` with this code:

```typescript
const response = await fetch(req.url, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(req.payload),
});
if (!response.ok) throw new Error(`HTTP ${response.status}`);
return response.status;
```

Three lines:

1. POST the payload to the URL with `fetch`. Both come from the `WebhookDelivery` input.
2. Throw if the response is not ok (4xx/5xx). Temporal sees that and retries.
3. Return the HTTP status code as the Activity's result.

The full version is in the **Solution** tab.

> This is a regular `async function`. There's no "standalone" decorator. Standalone vs. inside-a-Workflow is decided by *how the Activity is called*, not how it's defined.

---

## 2. Submit it as a Standalone Activity (~3 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
ts-node src/worker.ts
```

You should see:

```bash,nocopy
Worker running on task queue 'webhook-queue'
```

The Worker is now polling Temporal for tasks. Leave it running.

In the [button label="Terminal" background="#444CE7"](tab-2) tab, run the starter script:

```bash,run
ts-node src/sendStandalone.ts evt_001
```

You should see:

```bash,nocopy
Standalone Activity completed with status 200
```

Open `src/sendStandalone.ts` in the [button label="Exercise" background="#444CE7"](tab-0) tab. This call submits the durable job:

```typescript
await client.activity.execute(deliverWebhook, {
  args: [{ url: WEBHOOK_RECEIVER_URL, payload: { ... }, eventId }],
  id: `deliver-${eventId}`,
  taskQueue: TASK_QUEUE,
  startToCloseTimeout: '10 seconds',
});
```

One API call. The client tells Temporal, "run this Activity once and give me the result." There is no Workflow class in the script, and there is no broker, scheduler, or result store for you to deploy. Temporal persisted the job before acknowledging it, dispatched it to your Worker, and stored the result.

Open the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see one delivery recorded. The tab auto-refreshes every 2 seconds.

---

## 3. Inspect the job in the Temporal UI (~2 min)

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and switch to the **Standalone Activities** tab in the left nav. You should see `deliver-evt_001` listed as Completed.

Click into it. Temporal is now handling three things you would otherwise have to build or operate:

- **Addressable.** Every job has a stable ID (`deliver-evt_001` here). You can query its status, fetch its result, cancel it, or terminate it from the UI, CLI, SDK, or API.
- **Durable.** The job was persisted before your Worker even saw it. If the Worker had crashed mid-delivery, the same job would have been redispatched to a Worker polling the Task Queue on retry.
- **Observable.** Status, attempt count, the last error, and the result are visible without a separate logging pipeline.

That is the shape we want from a job queue: durable execution, retries, status, and results without extra infrastructure to operate.

---

## Check your understanding

> Your `deliverWebhook` job hits a transient 503 from the receiver on attempt 1. With Temporal's default `retryPolicy`, what happens?

<details>
<summary>Answer</summary>

Temporal sees the thrown error, waits the initial retry interval (1s by default), and dispatches the job again. The retry policy is exponential, so later failures wait longer. The job stays "Running" in the UI through every retry, and you can watch the attempt counter increment.

Contrast: in a traditional job queue, retry behavior is something you re-implement per service, with subtle differences each time.

</details>

## Coming up

The next modules tackle what happens when reality intrudes:

- **Module 02**: Idempotency and crash safety. Crash the Worker mid-delivery, watch the receiver show duplicates, then fix it.
- **Module 03**: Deduplication via ID reuse. Submit the same job ID twice and let Temporal return the existing handle.
- **Module 04**: Concurrency, rate limits, priority and fairness. Stop one busy tenant from slowing everyone else down.
- **Module 05**: Heartbeats and checkpointing. Resume long-running jobs from the last reported progress after a crash.
- **Module 06**: Same code runs anywhere. Call the same `deliverWebhook` Activity from a Workflow.

---

📝 **Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
