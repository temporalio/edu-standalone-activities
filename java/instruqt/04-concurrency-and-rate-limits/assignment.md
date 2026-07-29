---
slug: concurrency-and-rate-limits
id: doafxh6aofsz
type: challenge
title: Concurrency and rate limits
teaser: Cap your Worker's throughput so a large fan-out doesn't overwhelm the downstream
  service.
notes:
- type: text
  contents: |
    # Concurrency and rate limits (Java)

    Your Activity retries safely now. By default, Temporal dispatches Activities
    as fast as the Worker can pull them off the Task Queue. That is often faster
    than the service your Activity is calling can handle.

    The downstream service has a rate limit. POST faster than that limit and you
    get 429s back, the receiver throttles you, and your delivery latency goes up.

    The fix is one option on the Worker: setMaxWorkerActivitiesPerSecond. The Worker
    dispatches Activities at the configured pace. Everything else waits in the
    Task Queue on the server.

    ## What you'll do

    1. Run 60 deliveries with no rate cap. They all land in about a second.
    2. Switch the Webhook receiver into a "2 req/sec downstream" mode. Re-run. Watch real 429s land and Activities retry.
    3. Add the rate cap to the Worker. Re-run with the rate-limited receiver. The flood of 429s stops.
    4. See where Priority fits for ordering urgent work.
tabs:
- id: pyiehvn4xyrz
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
- id: uojjgz4oe1nc
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercise/04-concurrency-and-rate-limits
- id: iyj4gss0fvin
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/solution/04-concurrency-and-rate-limits
- id: tox1gttlhwfu
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercise/04-concurrency-and-rate-limits
- id: ylfneynlph9e
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercise/04-concurrency-and-rate-limits
- id: 0tlxggkydkpy
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Pace your jobs and prioritize urgent work

Many job queues make rate control the consumer's problem. One busy tenant can fan out a huge batch and slow everyone else down. If the queue has no rate controls, the consumer has to back off on its own or hammer the downstream API into 429s.

Standalone Activities give you both controls in one place: `setMaxWorkerActivitiesPerSecond` paces dispatch so a fan-out does not overwhelm the receiver, and `Priority` puts urgent jobs ahead of bulk ones when the queue is contended.

You'll do four things in this module:

1. Run 60 deliveries with no rate cap. They land in about a second.
2. Switch the Webhook receiver into a "2 req/sec downstream" mode. Re-run. Watch the 429s land and the Activities retry.
3. Cap the Worker at 2 dispatches per second. Re-run with the rate-limited receiver. The flood of 429s stops.
4. See where `Priority` fits, and where to explore it next.

The **Solution** tab has the finished code. Estimated time: 12 minutes.

---

## 1. Run 60 deliveries with no rate cap (~3 min)

In the [button label="Worker" background="#444CE7"](tab-4) tab, start the Worker:

```bash,run
# Start the Worker with no rate cap
gradle -q execute -PmainClass=webhook.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-3) tab, send 60 deliveries:

```bash,run
# Reset the receiver, fan out 60 deliveries, and time how long they take
scripts/reset-receiver.sh
time gradle -q execute -PmainClass=webhook.SendBulk -PappArgs=60
```

With no rate cap, the 60 deliveries should complete in **a second or two**. The Worker dispatches them as fast as its concurrency limit allows. You should see:

```bash,nocopy
14:35:02 INFO  webhook.SendBulk - All 60 deliveries completed.
```

Check the [button label="Webhook receiver" background="#444CE7"](tab-5) tab. `received_count` and `processed_count` should both reach 60, and the `received_at` timestamps will all be clustered tight together. The tab auto-refreshes every 2 seconds.

Open the [button label="Temporal UI" background="#444CE7"](tab-0) tab, **Standalone Activities**. All 60 `bulk-*` Activities should be **Completed** with start and end timestamps clustered in the same one- or two-second window.

---

## 2. Add a real rate limit on the receiver (~3 min)

Now cap the receiver at 2 req/sec and fan out 60 deliveries against it, using `SendBulkDemo` (`SendBulkDemo.java`). It uses separate `demo-*` IDs so leftover retries don't collide with the `bulk-*` IDs used in sections 1 and 4.

In the [button label="Terminal" background="#444CE7"](tab-3) tab:

```bash,run
# Cap the receiver at 2 req/sec (verified), then fan out 60 deliveries. Watch the 429s.
scripts/rate-limit.sh 2
scripts/reset-receiver.sh
gradle -q execute -PmainClass=webhook.SendBulkDemo -PappArgs=60
```

`rate-limit.sh` sets the cap and reads it back, so it fails loudly if the cap did not stick. You should see:

```bash,nocopy
Receiver rate limit is now 2 req/sec.
```

`SendBulkDemo` will hang because the Activities keep retrying on every 429. After about **5 seconds**, press **Ctrl+C**.

Check the [button label="Webhook receiver" background="#444CE7"](tab-5) tab. Only a handful of deliveries land at first; the rest get rejected with `429 Too Many Requests` and keep retrying. You should see `throttled_count` climbing well past `processed_count`, which is the direct evidence the cap is doing something. Open the [button label="Worker" background="#444CE7"](tab-4) tab and look for error lines ending in `HTTP 429`.

> **If `throttled_count` stays at 0**, the cap isn't in effect. Check `rate_limit` in the receiver tab: if it reads `0`, re-run `scripts/rate-limit.sh 2` and start the fan-out again. Leaving and re-entering this module resets the cap to `0`, so it has to be set in the same pass as the fan-out.

Open the [button label="Temporal UI" background="#444CE7"](tab-0) tab, **Standalone Activities**. Most of the `demo-*` Activities should be in **Running** state with the attempt count climbing.

> **What's happening:** Temporal's per-Activity retry policy is great for one Activity that fails. It can't solve a *fleet-wide* throughput problem because the next attempt of Activity 1 fights for the same downstream slot as attempt 1 of every other Activity. The fix has to pace the dispatch itself.

---

## 3. Add the rate cap (~2 min)

Open `Worker.java` in the [button label="Exercise" background="#444CE7"](tab-1) tab. There's a `TODO` comment above `factory.newWorker(...)`. Replace the plain `newWorker` call with a rate-capped one:

```java
WorkerOptions options = WorkerOptions.newBuilder()
        .setMaxConcurrentActivityExecutionSize(10)
        .setMaxWorkerActivitiesPerSecond(2)
        .build();
var worker = factory.newWorker(Webhook.TASK_QUEUE, options);
```

The Worker now dispatches at most 2 Activities per second. The full version is in the **Solution** tab.

> **Where does the excess go?** It waits in the Task Queue on the Temporal server. The Worker polls, and the server hands it work at the configured rate. Unscheduled work stays in the queue and nothing is lost.

---

## 4. Re-run with the rate cap (~3 min)

The receiver should still be capped at 2/sec from section 2. Dispatch at the same pace and watch the 429s vanish. The run block below re-applies the cap so this section works even if it got cleared.

Restart the Worker so it picks up the new config. In the [button label="Worker" background="#444CE7"](tab-4) tab, press **Ctrl+C**, then re-run:

```bash,run
# Restart the Worker with the 2/sec rate cap
gradle -q execute -PmainClass=webhook.Worker
```

You should see:

```bash,nocopy
14:36:44 INFO  webhook.Worker - Worker running on task queue "webhook-queue" (rate cap: 2/sec)
```

In the [button label="Terminal" background="#444CE7"](tab-3) tab, send another 60:

```bash,run
# Clear leftover demo Activities, reset the receiver, re-apply the cap, then send 60
scripts/stop-demo-and-reset.sh
scripts/rate-limit.sh 2
time gradle -q execute -PmainClass=webhook.SendBulk -PappArgs=60
```

`stop-demo-and-reset.sh` clears the receiver and stops any `demo-*` Activities still retrying from section 2.

At 2/sec, draining 60 deliveries takes about **30 seconds**. Open the [button label="Temporal UI" background="#444CE7"](tab-0) tab, **Standalone Activities**, and watch `bulk-*` Activities flip from **Running** to **Completed** about two per second.

The [button label="Webhook receiver" background="#444CE7"](tab-5) tab will show the `received_at` timestamps visibly spread out instead of clustering, with `processed_count` climbing by about two per second. The `throttled_count` should be a small number from the initial burst, then flat.

---

## 5. The other control: Priority (~2 min)

`setMaxWorkerActivitiesPerSecond` controls *how fast* dispatch happens. The companion control is **Priority** (`StartActivityOptions.setPriority`), which decides *what order* work runs in when the queue is contended: a lower priority key means higher priority, so urgent jobs can jump ahead of a backlog even when they arrive later.

We don't walk through Priority hands-on in this module, but it's worth exploring next: see [Task Queue Priority and Fairness](https://docs.temporal.io/develop/task-queue-priority-fairness) in the Temporal docs.

---

## Check your understanding

> Your downstream API has a hard rate limit of **100 req/sec**. You configure `setMaxWorkerActivitiesPerSecond(10)` on your Worker and deploy. Are you safe?

<details>
<summary>Reveal the answer</summary>

Safe but probably underutilizing.

10/sec is 10% of your downstream's headroom. Unless you have ~10 Workers each at 10/sec polling the same Task Queue (aggregating to 100/sec), you're leaving most of the downstream's capacity unused.

Two options:

- `setMaxWorkerActivitiesPerSecond` is **per Worker**. The aggregate across your Worker fleet is `N × maxWorkerActivitiesPerSecond`.
- `setMaxTaskQueueActivitiesPerSecond` (also on `WorkerOptions`) is **queue-wide**. It sets a hard cap regardless of Worker count. Use this when you can't predict how many Workers will be running.

</details>

---

## Coming up

**Module 05**: Heartbeats and checkpointing. Your jobs are fast and rate-capped. Next, long-running jobs report progress every few seconds and resume from the last checkpoint after a Worker crash.

---

**Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
