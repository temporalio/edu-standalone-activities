---
slug: heartbeats-and-checkpointing
type: challenge
title: Heartbeats and checkpointing
teaser: Resume a long-running Standalone Activity from where it left off after the
  Worker service goes down.
notes:
- type: text
  contents: |
    # Heartbeats and checkpointing

    A single Standalone Activity that processes a batch of webhook deliveries can take minutes. When the Worker's service goes down mid-batch, many job queues either lose in-flight progress or expect you to invent a checkpointing scheme for each job type.

    Standalone Activities have heartbeats built in. The Activity reports progress after each unit of work, and the Temporal server stores that value. If the attempt dies (Worker crash, machine reboot, deploy), the next attempt reads the last checkpoint back and resumes from there instead of redoing work.

    ## What you'll do

    1. Run a long-running Activity that delivers 10 webhooks. Bring the service down mid-batch. Watch the retry start from item 0, and the receiver record duplicates.
    2. Add one block to read the checkpoint on retry and skip items already delivered.
    3. Re-run, take the service down again, and watch the retry resume from the checkpoint with no duplicates.
tabs:
- title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/05-heartbeats-and-checkpointing/exercise
- title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/05-heartbeats-and-checkpointing/solution
- title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/05-heartbeats-and-checkpointing/exercise
- title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/05-heartbeats-and-checkpointing/exercise
- title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
- title: Interactive Diagram
  type: service
  hostname: workshop
  port: 9001
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Resume long-running jobs from the last checkpoint

Many job queues lose in-flight work when the Worker's service goes down. For a 30-second job that's annoying. For a 30-minute batch that's already half done, it is a real cost. The typical fix is to invent a per-job-type checkpointing scheme that lives in a side database.

Standalone Activities include heartbeats and checkpointing. Inside the Activity, `Activity.getExecutionContext().heartbeat(progress)` reports liveness and stores progress on the Temporal server. When the next attempt starts, it reads `getHeartbeatDetails(Integer.class)` and resumes from there. No side database required.

You'll do three things in this module:

1. Run a 10-item batch delivery Activity. Bring the service down mid-batch. Watch the retry start from item 0, and the receiver record duplicates.
2. Add one block to read the checkpoint on retry and skip items already delivered.
3. Re-run, take the service down again, and watch the retry resume from the checkpoint without duplicates.

The **Solution** tab has the finished code. Estimated time: 10 minutes.

---

## 1. See the bug: retry restarts from item 0 (~3 min)

Open `src/main/java/webhooks/WebhookActivitiesImpl.java` in the [button label="Exercise" background="#444CE7"](tab-0) tab. The `deliverWebhookBatch` Activity already calls `ctx.heartbeat(delivered)` after each item, so the server _has_ the progress data. What's missing is the read on retry.

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker (defined in `src/main/java/webhooks/Worker.java`):

```bash,run
# Start the Worker
mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send a 10-item batch with `src/main/java/webhooks/SendBatch.java` and bring the service down mid-run:

```bash,run
# Reset the receiver, submit a 10-item batch, bring the service down mid-batch
scripts/reset-receiver.sh
mvn -q compile exec:java -Dexec.mainClass=webhooks.SendBatch -Dexec.args="10" &
sleep 8 && scripts/kill-worker.sh
```

That sequence:

- Submits a batch of 10 items (1s delay between each = ~10s of delivery).
- Waits a few seconds so the first attempt delivers some items, then brings the service down.
- Leaves the `SendBatch` client waiting in the background.

### Observe the state while the service is down

Before restarting, look at where things stand:

- [button label="Webhook receiver" background="#444CE7"](tab-4): a few deliveries, the items that landed before the service went down.
- [button label="Temporal UI" background="#444CE7"](tab-5) then **Standalone Activities** then `deliver-batch-10`: the Activity is still listed as **Running**. Temporal has not given up on it. It is waiting for a Worker to come back.

Restart the Worker:

```bash,run
# Restart the Worker so the retry has somewhere to run
mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker
```

Return to the [button label="Terminal" background="#444CE7"](tab-2) tab and wait for the background client to finish:

```bash,run
# Wait for the background SendBatch client to finish
wait
```

After ~5 seconds, `heartbeatTimeout` fires on the server. No heartbeat for 5s means the attempt is dead, so Temporal triggers a retry and the new Worker picks it up. The retry runs the Activity body **from the top**, including items already delivered.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. `"processed_count"` should exceed 10. The items delivered before the service went down are recorded a second time because the retry started from item 0:

```json,nocopy
{
  "processed_count": 14
}
```

The receiver had no way to know these were duplicates: each batch item is a distinct delivery with its own `eventId`, so the receiver-side idempotency key from Module 02 wouldn't catch them. Resuming from a checkpoint is what avoids the re-delivery here.

> **What's happening:** the Activity heartbeated its progress on the first attempt, but the second attempt never reads the checkpoint. So it starts at `startIndex = 0` and redoes everything. The evidence is in the [button label="Worker" background="#444CE7"](tab-3) console (the `Delivered item N` lines start again from 0) and the receiver's count, not the Temporal UI. A Completed Standalone Activity does not display its per-attempt history.

Open the [button label="Interactive Diagram" background="#444CE7"](tab-6) tab to step through what just happened: the code on the left, the execution state on the right.

### What's a checkpoint?

Think of it like a video game checkpoint system.

The scenario: your Activity is processing 1,000 emails. It takes 10 minutes. Halfway through, the Worker's service goes down.

Without heartbeats for checkpoints, Temporal retries from zero. You reprocess the 500 emails you already sent.

Heartbeats are useful when an Activity is doing long, resumable work where restarting from zero would be wasteful or harmful.

**Quick check:** When should you use heartbeats? Select all that apply.

[x] Processing a large list (emails, records, files), skipping already-done items on retry
[x] Uploading or downloading a large file, resuming from the last byte offset instead of restarting
[x] Work where doing it twice causes problems (duplicate charges, duplicate emails)
[x] You need the Activity to detect that it was cancelled mid-loop (heartbeat throws an `ActivityCanceledException` when cancellation is requested)

---

## 2. Read the checkpoint on retry (~2 min)

Back in the [button label="Exercise" background="#444CE7"](tab-0) tab, find the `TODO` in `deliverWebhookBatch` in `src/main/java/webhooks/WebhookActivitiesImpl.java`. Replace:

```java
int startIndex = 0;
```

with:

```java
int startIndex = 0;
Optional<Integer> checkpoint = ctx.getHeartbeatDetails(Integer.class);
if (checkpoint.isPresent()) {
  startIndex = checkpoint.get();
  System.out.println("Resuming from checkpoint " + startIndex);
}
```

That's the fix. The full solution is in the **Solution** tab. Instruqt auto-saves your edits.

`getHeartbeatDetails(Integer.class)` returns whatever you passed to `ctx.heartbeat()` on the previous attempt, wrapped in an `Optional`. We pass a single number (`delivered`), so `checkpoint.get()` is the count of items already done. The `Optional` is empty on the first attempt (nothing has been heartbeated yet), so `startIndex` stays 0 there.

---

## 3. Verify the fix (~3 min)

Restart the Worker so it picks up the new code. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then re-run. `mvn compile exec:java` recompiles the edited Activity before it starts:

```bash,run
# Restart the Worker with the checkpoint fix
mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, repeat the take-the-service-down sequence:

```bash,run
# Same as section 1: reset, submit batch, bring the service down mid-run
scripts/reset-receiver.sh
mvn -q compile exec:java -Dexec.mainClass=webhooks.SendBatch -Dexec.args="10" &
sleep 8 && scripts/kill-worker.sh
```

Peek before restarting:

- [button label="Webhook receiver" background="#444CE7"](tab-4): a few deliveries. The first attempt heartbeated its progress.
- [button label="Temporal UI" background="#444CE7"](tab-5) then **Standalone Activities** then `deliver-batch-10`: still **Running**, waiting for a Worker.

Restart the Worker:

```bash,run
# Restart the Worker so the checkpoint-aware retry can run
mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker
```

Return to the [button label="Terminal" background="#444CE7"](tab-2) tab and wait:

```bash,run
# Wait for the background SendBatch client to finish. Should report exactly 10.
wait
```

The [button label="Webhook receiver" background="#444CE7"](tab-4) tab shows `"processed_count": 10`. No duplicates. On the retry the Activity read the checkpoint, jumped to that index, and finished the remaining items without redoing anything. The [button label="Worker" background="#444CE7"](tab-3) console shows the `Resuming from checkpoint` line on the second attempt.

> **The takeaway:** same Activity, same service outage, same restart. But the receiver sees each item exactly once. Heartbeating is how a long-running Activity saves progress before the next outage.

## Try the Interactive Diagram

Open the [button label="Interactive Diagram" background="#444CE7"](tab-6) tab. Switch between **Bug (Exercise)** and **Fixed (Solution)** to step through both attempts side by side: the code on the left, execution state on the right.

**Quick check:** When should you skip heartbeats? Select all that apply.

[x] The Activity is short (under ~10s), not worth the complexity
[x] The work is naturally idempotent and fast to redo, so just let it retry from scratch
[x] There's no meaningful "progress" to save (for example, a single API call)

---

## Handle cancellation cleanly

Heartbeating also delivers **cancellation**. When someone runs `temporal activity cancel deliver-batch-10` (or an enclosing Workflow cancels), Temporal can't interrupt your Java code directly. It sets a flag on the server, and the next `ctx.heartbeat()` call sees it and throws `ActivityCanceledException`.

Long-running Activities should catch it and exit cleanly:

```java
import io.temporal.client.ActivityCanceledException;

try {
  for (int i = startIndex; i < req.items.size(); i++) {
    // ... deliver item ...
    ctx.heartbeat(delivered);
  }
} catch (ActivityCanceledException e) {
  log.info("Cancelled after delivering {} items", delivered);
  throw e;
}
```

If you don't heartbeat, cancellation cannot reach the Activity at all. It will run to completion regardless.

---

## Check your understanding

> Your batch Activity has `heartbeatTimeout` set to `Duration.ofSeconds(5)` and processes one item per second. Mid-batch, the Worker hangs (deadlock, not crash). It stops calling heartbeat, but the process is still alive. What does Temporal do?

<details>
<summary>Answer</summary>

Temporal treats the attempt as dead after 5 seconds with no heartbeat, the same as a crash. It schedules a retry on whatever Worker picks it up next.

That's the point of `heartbeatTimeout`: it's the server's way to detect a stuck or dead attempt without waiting for the much longer `startToCloseTimeout`. Heartbeats are not just for storing progress. They are the liveness signal that lets the server route around a stuck Worker quickly.

</details>

## Coming up

**Module 06**: Same code runs anywhere. You've now used Standalone Activities for retries, idempotency, dedup, rate limits, and heartbeats. Final stop: take the same Activity code you've been writing and call it from a Workflow.

---

📝 **Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
