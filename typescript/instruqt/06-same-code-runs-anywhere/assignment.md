---
slug: same-code-runs-anywhere
id: ""
type: challenge
title: Same code runs anywhere
teaser: The same Activity you've been writing, now called from a Workflow. One Activity,
  two job types.
notes:
- type: text
  contents: |
    # Same code runs anywhere

    Traditional job queues can paint you into a corner. The queue runs jobs,
    orchestration lives somewhere else, and the code that ran in the queue often
    gets copied or rewritten when the work becomes a multi-step process.

    Standalone Activities give you a path forward. The Activity code you wrote
    in Modules 01-05 is a self-contained unit of durable work. Today you submit
    it as a top-level job. Tomorrow, when the job grows into a multi-step
    process, you can call the same Activity from a Workflow without rewriting it.

    This module proves it. The exact same `deliverWebhook` function is submitted
    two ways: directly via `client.activity.execute` (the Standalone Activity path
    you've used all tutorial) and via `client.workflow.execute` (a Workflow that
    calls the Activity as a step). Same Activity, two job types.
tabs:
- id: ""
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/06-same-code-runs-anywhere/exercise
- id: ""
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/06-same-code-runs-anywhere/solution
- id: ""
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/06-same-code-runs-anywhere/exercise
- id: ""
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/06-same-code-runs-anywhere/exercise
- id: ""
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: ""
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
---

# One Activity, two job types

You've written `deliverWebhook` once. Through five modules you've submitted it as a Standalone Activity and learned how it behaves under the Temporal platform: durably persisted, retried, dedupable, rate-capped, heartbeating.

Now the upgrade path: the same `deliverWebhook` function, unchanged, runs as a step inside a multi-step Workflow. No copy, no rewrite. Temporal handles both job types.

You'll do three things in this module:

1. Read `activities.ts`. It's the exact `deliverWebhook` Activity from Module 01.
2. Run `sendStandalone.ts`, which submits the Activity directly. You've done this 5 modules in a row.
3. Run `sendViaWorkflow.ts`, which submits the same Activity as a Workflow step. Compare the two in the Temporal UI.

Estimated time: 8 minutes.

---

## 1. Look at the Activity (~1 min)

Open `src/activities.ts` in the [button label="Exercise" background="#444CE7"](tab-0) tab. You'll recognize it. It's the `deliverWebhook` Activity you wrote in Module 01:

```typescript
export async function deliverWebhook(req: WebhookDelivery): Promise<number> {
  log.info('Delivering webhook for event', { eventId: req.eventId, url: req.url });
  const response = await fetch(req.url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req.payload),
  });
  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  return response.status;
}
```

Now open `src/workflow.ts`. A short Workflow that calls the same Activity:

```typescript
import { proxyActivities } from '@temporalio/workflow';
import type * as activities from './activities';

const { deliverWebhook } = proxyActivities<typeof activities>({
  startToCloseTimeout: '10 seconds',
});

export async function webhookWorkflow(req: WebhookDelivery): Promise<number> {
  return await deliverWebhook(req);
}
```

The Workflow proxies `deliverWebhook` from the same `activities.ts` module the standalone caller imports. The Activity does not know whether it is being invoked as a Standalone Activity or as a Workflow step.

---

## 2. Submit it as a Standalone Activity (~2 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
ts-node src/worker.ts
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send one standalone delivery:

```bash,run
ts-node src/sendStandalone.ts evt_001
```

You should see:

```bash,nocopy
Standalone Activity completed with status 200
```

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities**. You'll see `deliver-evt_001` listed as a completed Standalone Activity. This is the API surface you've used through all five previous modules.

---

## 3. Submit the same Activity as a Workflow step (~2 min)

In the [button label="Terminal" background="#444CE7"](tab-2) tab, run the second starter:

```bash,run
ts-node src/sendViaWorkflow.ts evt_002
```

You should see:

```bash,nocopy
Workflow completed with Activity returning status 200
```

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab. Look at both views:

- **Standalone Activities** tab: `deliver-evt_001` from step 2 is here.
- **Workflows** tab: `wf-evt_002` is here, and clicking it shows the Activity as a step in its history.

The same `deliverWebhook` Activity ran in both cases. The receiver recorded two distinct deliveries, one per submission. From the Activity's perspective, there is no difference between the two calls.

> **What's happening:** the Worker registered both the Activity and the Workflow. Temporal's server routes a `client.activity.execute` call to dispatch the Activity directly; a `client.workflow.execute` call dispatches the Workflow, which then dispatches the Activity. Same code, two submission paths, two job types.

---

## Why this matters

Every Activity you've written in this tutorial is a reusable building block:

- `deliverWebhook` could be step 3 of an order-processing Workflow tomorrow, between "charge the card" and "send confirmation email."
- The `deliverWebhookBatch` from Module 05 could be a long-running step inside a daily-rollup Workflow.
- The `idConflictPolicy`, `maxActivitiesPerSecond`, and `Priority` knobs you learned still apply when those Activities are called from inside a Workflow.

This is the upgrade path traditional job queues don't have. When the requirements grow from "one durable POST" to "charge the card, then reserve inventory, then notify ops if any step fails," you compose the Activities you already wrote into a Workflow. You don't rewrite them in another tool.

---

## Coinbase did exactly this

At Replay 2026, Coinbase described migrating their custom Background Jobs Service onto Standalone Activities. That service handles 200 to 600 million jobs per day across 186 namespaces. The same Activity code can run standalone today and be composed into Workflows tomorrow, which lets one system replace a separate job queue and orchestrator.

The full talk: [Standalone Activities for durable job processing, Coinbase at Replay 2026](https://www.youtube.com/watch?v=zsF5Y-IOMOw).

---

## Wrap-up

What you can now do with Standalone Activities in TypeScript:

- **`client.activity.execute` / `client.activity.start`**: submit an Activity as a top-level durable job, no Workflow required.
- **Idempotency keys for external writes**: make retries safe by giving the receiver a stable dedup key.
- **`ActivityIdConflictPolicy.USE_EXISTING`**: let the server handle duplicate `start` calls instead of erroring.
- **`maxActivitiesPerSecond`** on the Worker: cap dispatch rate to protect the downstream service.
- **`Priority`** on `client.activity.start`: push urgent work ahead of bulk when the queue is contended.
- **`Context.current().heartbeat(progress)` + `heartbeatTimeout`**: report progress from long-running jobs and resume from the last checkpoint after a crash.
- **The same Activity, called from a Workflow**: use the Activity in multi-step orchestration without rewriting it.

Temporal lets you start with a job and move to a Workflow when the work grows. The Activity code you wrote still comes with you.

Click **Check** to finish the track.

---

📝 **Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
