---
slug: idempotency-and-crash-safety
id: ""
type: challenge
title: Idempotency and crash safety
teaser: Crash the Worker mid-flight; watch duplicate deliveries; fix them with one
  line.
notes:
- type: text
  contents: |
    # Making retries safe with idempotency

    Temporal automatically retries Activities that fail. That is almost always what you want, unless the Activity already did something visible before it failed.

    A concrete example: your `deliverWebhook` Activity POSTs to the receiver's URL. The receiver gets the request and processes it. Then something errors out: the receiver returns a 500, the network drops, or the Worker crashes after the POST. Temporal sees no successful completion, so it retries the whole Activity, POST included. The receiver gets the same delivery twice.

    This is at-least-once delivery. To get effectively-once side effects, your Activity needs to be **idempotent**: safe to run more than once with the same input. The usual way is to send an idempotency key with each request and let the receiver dedupe.

    ## What you'll do

    1. Run an Activity that POSTs a webhook, then errors out on its first two attempts. Watch the Webhook receiver process 3 deliveries for one logical event.
    2. Add a one-line idempotency key to the POST. Re-run. Watch the Webhook receiver receive 3 requests but process only 1 delivery.
tabs:
- id: ""
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- id: ""
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/solution
- id: ""
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- id: ""
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
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

# Make retries safe with idempotency

Many job queues leave retry behavior to each service, and they often don't help when a job already did something before it failed. The POST landed. The card was charged. The email went out. The Activity errors before it can report success. The retry runs the side effect again.

Standalone Activities retry for you automatically, but at-least-once delivery is still at-least-once. You'll see this happen, then fix it with idempotency on the receiver side.

You'll do three things in this module:

1. Reproduce the bug: an Activity that POSTs and then fails on its first two attempts. The Webhook receiver processes 3 deliveries for one logical request.
2. Add a one-line idempotency key derived from the webhook event id. Re-run. The Webhook receiver receives 3 requests but processes only 1 delivery.
3. Understand why the key has to be stable across retries (and not `crypto.randomUUID()`).

The **Solution** tab has the finished code. Estimated time: 10 minutes.

---

## 1. Reproduce the bug (~3 min)

Open `src/activities.ts` in the [button label="Exercise" background="#444CE7"](tab-0) tab. The Activity is set up to POST the webhook, then throw an `ApplicationFailure` on its first two attempts. This simulates a transient failure after the side effect already happened: the receiver processed the request, but the Worker errored before Temporal heard "done." On each retry Temporal re-runs the Activity body, POST included.

There's a `TODO` above the `headers` object. Leave it alone for now so you can see what goes wrong before the fix.

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
ts-node src/worker.ts
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send one delivery:

```bash,run
scripts/reset-receiver.sh
ts-node src/sendStandalone.ts evt_buggy
```

(`scripts/reset-receiver.sh` clears the Webhook receiver so each run starts from zero.)

The Activity fails on attempts 1 and 2, succeeds on attempt 3. Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see **3 requests received** and **3 deliveries processed** for `evt_buggy`:

```json,nocopy
{
  "received_count": 3,
  "processed_count": 3,
  "deduped_count": 0
}
```

The receiver had no way to know these were duplicates of the same logical event.

---

## 2. Add the idempotency key (~2 min)

Back in the [button label="Exercise" background="#444CE7"](tab-0) tab, find the `TODO` in `deliverWebhook`. Add this to the `headers` object:

```typescript
'Idempotency-Key': `webhook:${req.eventId}`,
```

The full solution is in the **Solution** tab.

`req.eventId` is the logical event this Activity is delivering. That key is **stable across retries** of the same logical webhook. The Webhook receiver caches by this header: if it sees a key it's seen before, it returns the cached response and doesn't process a new delivery.

> **Why not `crypto.randomUUID()`?** UUIDs are different every time you call them. Each retry would generate a fresh key, the receiver would see N different keys for N retries of one logical request, and your "idempotency" would dedupe nothing. The key has to be deterministic across retries.

---

## 3. Verify the fix (~3 min)

The Worker is still running the old code. Restart it so it picks up the change. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then re-run:

```bash,run
ts-node src/worker.ts
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send another delivery with the fix in place:

```bash,run
scripts/reset-receiver.sh
ts-node src/sendStandalone.ts evt_fixed
```

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see **3 requests received** and **1 delivery processed** for `evt_fixed`:

```json,nocopy
{
  "received_count": 3,
  "processed_count": 1,
  "deduped_count": 2
}
```

Three POSTs still landed at the receiver because the Activity still retried three times. The receiver saw the same idempotency key on each one, so it returned a cached response to attempts 2 and 3 without processing new deliveries.

> **The takeaway:** at-least-once delivery (Temporal) + idempotency (your Activity + receiver) = effectively at-most-once side effect. Temporal can't guarantee exactly-once on its own; that's a property your Activity and the system it talks to have to provide together.

---

## Bound the retries in production

Temporal's **default `retryPolicy` is unbounded**. It starts at 1 second, backs off up to 100 seconds, and has no maximum attempt count. Useful for transient failures, but risky for permanently broken receivers. Pass an explicit `retryPolicy` to bound it:

```typescript
await client.activity.execute(deliverWebhook, {
  // ...
  retry: { maximumAttempts: 5 },
});
```

For permanent failures the Activity itself can recognize, throw `ApplicationFailure.nonRetryable('message')`. Temporal stops retrying immediately instead of using up your `maximumAttempts`.

---

## Check your understanding

> Your Activity body generates a random discount code and builds the `Idempotency-Key` from it. What goes wrong on a retry, and how do you fix it?

<details>
<summary>Answer</summary>

Each retry generates a different random code, so the idempotency key changes per attempt. The receiver sees N different keys for the same logical request and accepts all N. The "idempotency" dedupes nothing.

The fix is to make the key deterministic across retries: derive it from input fields the caller already chose (e.g. `req.eventId`), or for workflow-bound Activities, use `workflowRunId + activityId`. If you need a random code as part of the side effect, generate it in the caller and pass it in as Activity input.

</details>

## Coming up

**Module 03**: Deduplication via ID reuse. You've made retries safe on the *receiver* side. Next, have Temporal reject duplicate submissions before a Worker sees them.

---

📝 **Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
