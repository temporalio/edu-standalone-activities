---
slug: heartbeats-and-checkpointing
id: zyy8i4fwnq0y
type: challenge
title: Heartbeats and checkpointing
teaser: Resume a long-running Standalone Activity from where it left off after a Worker
  crash.
notes:
- type: text
  contents: |
    # Heartbeats and checkpointing (Java)

    A single Standalone Activity that processes a batch of webhook deliveries can
    take minutes. When the Worker crashes mid-batch, many job queues either lose
    in-flight progress or expect *you* to invent a checkpointing scheme for each
    job type.

    Standalone Activities have heartbeats built in. The Activity calls
    context.heartbeat(progress) after each unit of work; the Temporal server stores
    that value. If the attempt dies (Worker crash, machine reboot, deploy), the
    next attempt reads the heartbeat details with context.getHeartbeatDetails(...) and
    resumes from the last reported checkpoint instead of redoing work.

    ## What you'll do

    1. Run a long-running Activity that delivers 10 webhooks. Bring the service down mid-batch. Watch the retry start from item 0, and the receiver records duplicates.
    2. Add one block to read the heartbeat details on retry and skip items already delivered.
    3. Re-run, kill again, and watch the retry resume from the checkpoint with no duplicates.
tabs:
- id: 10gmm9makuhm
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
- id: z3tksg9lgymi
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercise/05-heartbeats-and-checkpointing
- id: ikcqje6qgkpu
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/solution/05-heartbeats-and-checkpointing
- id: hwos0lypthow
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercise/05-heartbeats-and-checkpointing
- id: jrdwtidax1mq
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercise/05-heartbeats-and-checkpointing
- id: oyxvqomvgw8p
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: jelclzctzbef
  title: Interactive Diagram
  type: service
  hostname: workshop
  port: 9001
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Resume long-running jobs from the last checkpoint

Many job queues lose in-flight work when the Worker crashes. For a 30-second job that's annoying. For a 30-minute batch that's already half done, it is a real cost. The typical fix is to invent a per-job-type checkpointing scheme that lives in a side database.

Standalone Activities include heartbeats and checkpointing. `context.heartbeat(progress)` reports liveness and stores progress on the Temporal server. When the next attempt starts, it reads the heartbeat details and resumes from there. No side database required.

You'll do three things in this module:

1. Run a 10-item batch delivery Activity. Bring the service down mid-batch. Watch the retry start from item 0, and the receiver records duplicates.
2. Add one block to read the heartbeat details on retry and skip items already delivered.
3. Re-run, kill again, and watch the retry resume from the checkpoint without duplicates.

The **Solution** tab has the finished code. Estimated time: 10 minutes.

---

## 1. See the bug: retry restarts from item 0 (~3 min)

Open `WebhookActivitiesImpl.java` in the [button label="Exercise" background="#444CE7"](tab-1) tab. The `deliverWebhookBatch` Activity has two `TODO` comments: TODO 1 (read the checkpoint on retry) and TODO 2 (record a heartbeat after each item). Neither is filled in yet, so the server never receives progress and a retry has nothing to resume from.

In the [button label="Worker" background="#444CE7"](tab-4) tab, start the Worker:

```bash,run
# Start the Worker
gradle -q execute -PmainClass=webhook.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-3) tab, send a 10-item batch and bring the service down mid-run:

```bash,run
# Reset the receiver, submit a 10-item batch, bring the service down once ~5 items land
scripts/reset-receiver.sh
gradle -q execute -PmainClass=webhook.SendBatch -PappArgs=10 > /tmp/sendbatch.log 2>&1 &
scripts/kill-worker-after.sh 5
```

That sequence:

- Submits a batch of 10 items (1s delay between each = ~10s total).
- Waits until 5 items have actually landed at the receiver, then brings the service down. Five rather than four because the SDK only flushes a heartbeat to the server every ~4s, and section 3 needs at least one flush on record. It polls instead of sleeping a fixed time because Gradle can take several seconds to start.
- Leaves the `SendBatch` client waiting in the background.

### Observe the state while the Worker is down

Before restarting, look at where things stand:

- [button label="Webhook receiver" background="#444CE7"](tab-5): 5 deliveries, the items that landed before the kill.
- [button label="Temporal UI" background="#444CE7"](tab-0), **Standalone Activities**, `deliver-batch-10`: the Activity is still listed as **Running**. Temporal has not given up on it. It is waiting for a Worker to come back.

Restart the Worker:

```bash,run
# Restart the Worker so the retry has somewhere to run
gradle -q execute -PmainClass=webhook.Worker
```

Return to the [button label="Terminal" background="#444CE7"](tab-3) tab and wait for the background client to finish:

```bash,run
# Wait for the background SendBatch client, then read its captured output
wait
cat /tmp/sendbatch.log
```

After ~5 seconds, the Activity's heartbeat timeout fires on the server. No heartbeat for 5s means the attempt is dead, so Temporal triggers a retry and the new Worker picks it up. The retry replays the Activity body **from the top**, including items already delivered.

Check the [button label="Webhook receiver" background="#444CE7"](tab-5) tab. `"processed_count"` should exceed 10. Items 0 through 4 are recorded twice because the retry started from item 0:

```json,nocopy
{
  "processed_count": 15
}
```

The receiver had no way to know these were duplicates: each batch item is a distinct delivery with its own `eventId`, so the receiver-side idempotency key from Module 02 wouldn't catch them. Resuming from a checkpoint is what avoids the re-delivery here.

> **What's happening:** neither TODO in `WebhookActivitiesImpl.java` is filled in, so the first attempt never reports progress, and the second attempt never reads it back. It starts at `startIndex = 0` and redoes everything.

Open the [button label="Interactive Diagram" background="#444CE7"](tab-6) tab to step through what just happened: the code on the left, the execution state on the right.

### What's a checkpoint?

Think of it like a video game checkpoint system.

The scenario: your Activity is processing 1,000 emails. It takes 10 minutes. Halfway through, the Worker crashes.

Without heartbeats for checkpoints, Temporal retries from zero. You reprocess the 500 emails you already sent.

Heartbeats are useful when an Activity is doing long, resumable work where restarting from zero would be wasteful or harmful.

> **Quick check:** Which kinds of Activity work are worth heartbeating?

<details>
<summary>Reveal the answer</summary>

All four of these:

- Processing a large list (emails, records, files): skip already-done items on retry.
- Uploading or downloading a large file: resume from the last byte offset instead of restarting.
- Work where doing it twice causes problems (duplicate charges, duplicate emails).
- Work that needs to detect it was cancelled mid-loop. `context.heartbeat(...)` throws `ActivityCompletionException` once cancellation is requested.

</details>

---

## 2. Read the checkpoint on retry (~2 min)

Back in the [button label="Exercise" background="#444CE7"](tab-1) tab, find TODO 1 and TODO 2 in `WebhookActivitiesImpl.java`.

For TODO 1, add this block just below `int startIndex = 0;`:

```java
Optional<Integer> checkpoint = context.getHeartbeatDetails(Integer.class);
if (checkpoint.isPresent()) {
    startIndex = checkpoint.get();
    log.info("Resuming from checkpoint startIndex={} attempt={}",
            startIndex, context.getInfo().getAttempt());
}
```

The `log.info` is what you'll look for in the Worker console in section 3 to confirm the retry actually read the checkpoint.

For TODO 2, scroll down into the delivery `for` loop. It sits below the status-check block, right after `delivered++;`. Add this line there:

```java
context.heartbeat(delivered);
```

You'll also need to import `java.util.Optional`. The full solution is in the **Solution** tab.

The heartbeat details are whatever you passed to `context.heartbeat(...)` in the previous attempt. We pass a single number (`delivered`), so the decoded checkpoint is the count of items already done.

---

## 3. Verify the fix (~3 min)

Restart the Worker so it picks up the new code. In the [button label="Worker" background="#444CE7"](tab-4) tab, press **Ctrl+C**, then re-run:

```bash,run
# Restart the Worker with the checkpoint fix
gradle -q execute -PmainClass=webhook.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-3) tab, repeat the kill-mid-batch dance:

```bash,run
# Same as section 1: reset, submit batch, kill mid-run to trigger the retry
scripts/reset-receiver.sh
gradle -q execute -PmainClass=webhook.SendBatch -PappArgs=10 > /tmp/sendbatch.log 2>&1 &
scripts/kill-worker-after.sh 5
```

Peek before restarting:

- [button label="Webhook receiver" background="#444CE7"](tab-5): about 5 deliveries. The first attempt heartbeated its progress.
- [button label="Temporal UI" background="#444CE7"](tab-0), **Standalone Activities**, `deliver-batch-10`: still **Running**, waiting for a Worker.

Restart the Worker:

```bash,run
# Restart the Worker so the checkpoint-aware retry can run
gradle -q execute -PmainClass=webhook.Worker
```

Return to the [button label="Terminal" background="#444CE7"](tab-3) tab and wait. The client is running in the background, so its output was redirected to a file rather than printed straight to this terminal:

```bash,run
# Wait for the background SendBatch client, then read its captured output
wait
cat /tmp/sendbatch.log
```

You should see:

```bash,nocopy
14:41:53 INFO  webhook.SendBatch - Batch delivery completed: 10 items delivered.
```

That number is always 10, because the Activity returns the total item count once it finishes. The interesting number is on the receiver: the [button label="Webhook receiver" background="#444CE7"](tab-5) tab shows `"processed_count"` around 11, down from the 15 you saw without a checkpoint. Only the item at the crash boundary gets redone instead of all five. The retry read the heartbeat details, jumped near the checkpoint index, and finished the remaining items instead of redoing the whole batch.

The Java SDK throttles how often heartbeats actually reach the server, capping updates to roughly 80% of the heartbeat timeout. That means the checkpoint the server has on file can lag a beat behind what the Activity already delivered locally, so do not be surprised if one or two items right at the crash boundary show up twice. That is still far better than redoing the whole batch, and it is why heartbeating is worth using even though it is not a perfect exactly-once guarantee.

Look at the Worker console logs for the second attempt. You should see a line containing `Resuming from checkpoint`, with a `startIndex` close to wherever the first attempt got interrupted and `attempt=2` (the exact number depends on heartbeat throttling timing).

That log line, together with the receiver's counts, is your evidence the resume worked: instead of restarting at item 0, the retry picked up from somewhere in the middle. A **Completed** Standalone Activity's record in the Temporal UI does not show a per-attempt breakdown or prove it was retried; the attempt count is only visible while the Activity is **Running**.

> **The takeaway:** same Activity, same kill, same restart. But the receiver sees most items exactly once instead of the whole batch twice. Heartbeating is how a long-running Activity saves progress before the next crash, even if the last checkpoint the server has can lag slightly behind due to throttling.

## Try Interactive Diagram

Open the [button label="Interactive Diagram" background="#444CE7"](tab-6) tab. Switch between **Bug (Exercise)** and **Fixed (Solution)** to step through both attempts side by side: the code on the left, execution state on the right.

> **Quick check:** When is heartbeating not worth it?

<details>
<summary>Reveal the answer</summary>

All three of these:

- The Activity is short (under ~10s), so the complexity doesn't pay for itself.
- The work is naturally idempotent and fast to redo, so letting it retry from scratch is fine.
- There's no meaningful "progress" to save, such as a single API call.

</details>

---

## Handle cancellation cleanly

Heartbeating also delivers **cancellation**. When someone runs `temporal activity cancel deliver-batch-10` (or an enclosing Workflow cancels), Temporal can't interrupt your Java code directly. It sets a flag on the server, and the next `context.heartbeat(...)` call throws `ActivityCompletionException`.

Long-running Activities should let that exception propagate so the attempt ends promptly:

```java
delivered++;
// heartbeat() throws ActivityCompletionException when cancellation has been requested.
// Let it propagate (don't swallow it) to end the Activity cleanly.
context.heartbeat(delivered);
```

If you don't heartbeat, cancellation cannot reach the Activity at all. It will run to completion regardless.

---

## Check your understanding

> Your batch Activity has a 5-second heartbeat timeout and processes one item per second. Mid-batch, the Worker hangs (deadlock, not crash). It stops calling `context.heartbeat(...)`, but the process is still alive. What does Temporal do?

<details>
<summary>Reveal the answer</summary>

Temporal treats the attempt as dead after 5 seconds with no heartbeat, the same as a crash. It schedules a retry on whatever Worker picks it up next.

That's the point of the heartbeat timeout: it's the server's way to detect a stuck or dead attempt without waiting for the much longer start-to-close timeout. Heartbeats are not just for storing progress. They are the liveness signal that lets the server route around a stuck Worker quickly.

</details>

## Coming up

**Module 06**: Same code runs anywhere. You've now used Standalone Activities for retries, idempotency, dedup, rate limits, and heartbeats. Final stop: take the same Activity code you've been writing and call it from a Workflow.

---

**Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
