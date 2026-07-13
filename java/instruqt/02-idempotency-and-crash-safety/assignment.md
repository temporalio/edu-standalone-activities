---
slug: idempotency-and-crash-safety
type: challenge
title: Idempotency and crash safety
teaser: Crash the Worker mid-flight; watch duplicate deliveries; fix them with one
  line.
notes:
- type: text
  contents: |
    # Making retries safe with idempotency

    Temporal automatically retries Activities that fail. That is almost always what you want, unless the Activity already did something visible before it failed.

    A concrete example: your deliverWebhook Activity POSTs to the receiver's URL. The receiver gets the request and processes it. Then something errors out: the receiver returns a 500, the network drops, or the Worker goes down after the POST. Temporal sees no successful completion, so it retries the whole Activity, POST included. The receiver gets the same delivery twice.

    This is at-least-once delivery. To get effectively-once side effects, your Activity needs to be idempotent: safe to run more than once with the same input. The usual way is to send an idempotency key with each request and let the receiver dedupe.

    ## What you'll do

    1. Run an Activity that POSTs a webhook, then errors out on its first two attempts. Watch the Webhook receiver process 3 deliveries for one logical event.
    2. Add a one-line idempotency key to the POST. Re-run. Watch the Webhook receiver receive 3 requests but process only 1 delivery.
tabs:
- title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/solution
- title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Make retries safe with idempotency

Many job queues leave retry behavior to each service, and they often don't help when a job already did something before it failed. The POST landed. The card was charged. The email went out. The Activity errors before it can report success. The retry runs the side effect again.

Standalone Activities retry for you automatically, but at-least-once delivery is still at-least-once. You'll see this happen, then fix it with idempotency on the receiver side.

<details>
<summary><strong>What is idempotency, and when do you need it?</strong></summary>

An operation is **idempotent** if running it multiple times with the same input produces the same outcome as running it once. A GET request is naturally idempotent. A POST that creates a record or sends a notification is not, unless you build that property in.

**When you need it with Temporal:**

Temporal guarantees an Activity runs to completion *at least once*. If the Activity throws before it returns, Temporal retries the entire Activity body. Any side effect inside the body (a network call, a database write, a charge) runs again on every attempt.

You need an idempotency key when the Activity calls an external system and that call is not safe to repeat. The pattern:

1. Derive a key from stable input, not from a random value generated inside the Activity. `req.eventId` is stable across retries. `java.util.UUID.randomUUID()` is not.
2. Send the key with every request (`Idempotency-Key` header, a request field, etc.).
3. The receiver caches the result by key and returns the cached response on duplicate requests without re-executing the side effect.

**When you don't need it:**

Read-only operations, and writes that are naturally idempotent (e.g. setting a field to a specific value rather than incrementing it) do not require a key.

</details>

You'll do three things in this module:

1. Reproduce the bug: an Activity that POSTs and then fails on its first two attempts. The Webhook receiver processes 3 deliveries for one logical request.
2. Add a one-line idempotency key derived from the webhook event id. Re-run. The Webhook receiver receives 3 requests but processes only 1 delivery.
3. Understand why the key has to be stable across retries (and not a fresh random UUID).

The **Solution** tab has the finished code. Estimated time: 10 minutes.

<iframe src="https://raw.githack.com/temporalio/edu-standalone-activities/standalone-activities-java-instruqt-skill/docs/java-idempotency-demo/index.html" width="100%" height="520" frameborder="0" style="border: 0; border-radius: 8px; margin: 0.5em 0;"></iframe>

---

## 1. Reproduce the bug (~3 min)

Open `src/main/java/webhooks/WebhookActivitiesImpl.java` in the [button label="Exercise" background="#444CE7"](tab-0) tab. The `deliverWebhook` Activity is set up to POST the webhook, then throw a `RuntimeException` on its first two attempts. This simulates a transient failure after the side effect already happened: the receiver processed the request, but the Worker errored before Temporal heard "done." On each retry Temporal re-runs the Activity body, POST included.

There's a `TODO` in the `HttpRequest.newBuilder()` chain. Leave it alone for now so you can see what goes wrong before the fix.

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker (defined in `src/main/java/webhooks/Worker.java`):

```bash,run
# Start the Worker
mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send one delivery with `src/main/java/webhooks/SendStandalone.java`:

```bash,run
# Clear the receiver, then submit one delivery (will fail twice then succeed)
scripts/reset-receiver.sh
mvn -q compile exec:java -Dexec.mainClass=webhooks.SendStandalone -Dexec.args="evt_buggy"
```

(`scripts/reset-receiver.sh` clears the Webhook receiver so each run starts from zero.)

The Activity fails on attempts 1 and 2, succeeds on attempt 3. The Worker tab prints a `Simulated transient failure on attempt N` line for each failed attempt. Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see **3 requests received** and **3 deliveries processed** for `evt_buggy`:

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

Back in the [button label="Exercise" background="#444CE7"](tab-0) tab, find the `TODO` in `deliverWebhook` in `src/main/java/webhooks/WebhookActivitiesImpl.java`. Add this line to the `HttpRequest.newBuilder()` chain, right after the `Content-Type` header:

```java
.header("Idempotency-Key", "webhook:" + req.eventId)
```

The full solution is in the **Solution** tab. Instruqt auto-saves your edits.

`req.eventId` is the logical event this Activity is delivering. That key is **stable across retries** of the same logical webhook. The Webhook receiver caches by this header: if it sees a key it's seen before, it returns the cached response and doesn't process a new delivery.

> **Why not a fresh random UUID?** `java.util.UUID.randomUUID()` returns a different value every time you call it. Each retry would generate a fresh key, the receiver would see N different keys for N retries of one logical request, and your "idempotency" would dedupe nothing. The key has to be deterministic across retries.

---

## 3. Verify the fix (~3 min)

The Worker is still running the old code. Restart it so it picks up the change. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then re-run the command below. `mvn compile exec:java` recompiles the edited Activity before it starts, so the running Worker picks up your change:

```bash,run
# Restart the Worker to pick up the idempotency-key change
mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send another delivery with the fix in place:

```bash,run
# Clear the receiver, then re-run with the idempotency key in place
scripts/reset-receiver.sh
mvn -q compile exec:java -Dexec.mainClass=webhooks.SendStandalone -Dexec.args="evt_fixed"
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

> **The takeaway:** at-least-once delivery (Temporal) + idempotency (your Activity + receiver) = effectively-once side effect. Temporal can't guarantee exactly-once on its own; that's a property your Activity and the system it talks to have to provide together.

---

## Bound the retries in production

Temporal's **default retry policy is unbounded**. It starts at 1 second, backs off up to 100 seconds, and has no maximum attempt count. Useful for transient failures, but risky for permanently broken receivers. Pass an explicit retry policy on the `StartActivityOptions` to bound it:

```java
StartActivityOptions options =
    StartActivityOptions.newBuilder()
        .setId("deliver-" + eventId)
        .setTaskQueue(Shared.TASK_QUEUE)
        .setStartToCloseTimeout(Duration.ofSeconds(10))
        .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(5).build())
        .build();
```

For permanent failures the Activity itself can recognize, throw `ApplicationFailure.newNonRetryableFailure("message", "type")`. Temporal stops retrying immediately instead of using up your maximum attempts.

---

## Check your understanding

> Your Activity body generates a random discount code and builds the `Idempotency-Key` from it. What goes wrong on a retry, and how do you fix it?

<details>
<summary>Answer</summary>

Each retry generates a different random code, so the idempotency key changes per attempt. The receiver sees N different keys for the same logical request and accepts all N. The "idempotency" dedupes nothing.

The fix is to make the key deterministic across retries: derive it from input fields the caller already chose (e.g. `req.eventId`), or for Workflow-bound Activities, use `workflowRunId + activityId`. If you need a random code as part of the side effect, generate it in the caller and pass it in as Activity input.

</details>

## Coming up

**Module 03**: Deduplication via ID reuse. You've made retries safe on the *receiver* side. Next, have Temporal reject duplicate submissions before a Worker sees them.

---

📝 **Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
