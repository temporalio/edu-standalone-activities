---
slug: concurrency-and-rate-limits
id: ups1jnyh57cx
type: challenge
title: Concurrency and rate limits
teaser: Cap your Worker's throughput so a large fan-out doesn't overwhelm the downstream
  service.
notes:
- type: text
  contents: |
    # Concurrency and rate limits

    Your Activity retries safely now. By default, Temporal dispatches Activities
    as fast as the Worker can pull them off the task queue. That is often faster
    than the service your Activity is calling can handle.

    The downstream service has a rate limit. POST faster than that limit and you
    get 429s back, the receiver throttles you, and your delivery latency goes up.

    The fix is one option on the Worker: `maxActivitiesPerSecond`. The Worker
    dispatches Activities at the configured pace. Everything else waits in the
    task queue on the server.

    ## What you'll do

    1. Run 60 deliveries with no rate cap. They all land in about a second.
    2. Switch the Webhook receiver into a "2 req/sec downstream" mode. Re-run. Watch real 429s land and Activities retry.
    3. Add `maxActivitiesPerSecond: 2` to the Worker. Re-run with the rate-limited receiver. The flood of 429s stops.
    4. See where Priority fits for ordering urgent work.
tabs:
- id: kdehhyphjpmx
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/04-concurrency-and-rate-limits/exercise
- id: eriai62zgbrp
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/04-concurrency-and-rate-limits/solution
- id: 8cqqrgcef5c4
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/04-concurrency-and-rate-limits/exercise
- id: n6r2azu6hrth
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/04-concurrency-and-rate-limits/exercise
- id: uvaxnrilsw7i
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: j4uizv5cwsvm
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Pace your jobs and prioritize urgent work

Many job queues make rate control the consumer's problem. One busy tenant can fan out a huge batch and slow everyone else down. If the queue has no rate controls, the consumer has to back off on its own or hammer the downstream API into 429s.

Standalone Activities give you both controls in one place: `maxActivitiesPerSecond` paces dispatch so a fan-out does not overwhelm the receiver, and `Priority` puts urgent jobs ahead of bulk ones when the queue is contended.

You'll do four things in this module:

1. Run 60 deliveries with no rate cap. They land in about a second.
2. Switch the Webhook receiver into a "2 req/sec downstream" mode. Re-run. Watch the 429s land and the Activities retry.
3. Cap the Worker at 2 dispatches per second. Re-run with the rate-limited receiver. The flood of 429s stops.
4. See where `Priority` fits, and where to explore it next.

The **Solution** tab has the finished code. Estimated time: 12 minutes.

---

## 1. Run 60 deliveries with no rate cap (~3 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
# Start the Worker with no rate cap
ts-node src/worker.ts
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send 60 deliveries:

```bash,run
# Reset the receiver, fan out 60 deliveries, and time how long they take
scripts/reset-receiver.sh
time ts-node src/sendBulk.ts 60
```

With no rate cap, the 60 deliveries should complete in **a second or two**. The Worker dispatches them as fast as its concurrency limit allows.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. `received_count` and `processed_count` should both reach 60, and the `received_at` timestamps will all be clustered tight together. The tab auto-refreshes every 2 seconds.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities**. All 60 `bulk-*` Activities should be **Completed** with start and end timestamps clustered in the same one- or two-second window.

---

## 2. Add a real rate limit on the receiver (~3 min)

In the [button label="Terminal" background="#444CE7"](tab-2) tab, turn on a 2 req/sec cap at the receiver:

```bash,run
# Enable a 2 req/sec cap on the receiver
curl -fsS -X POST "http://localhost:9000/_rate_limit?limit=2"
```

Now send 60 deliveries against the rate-limited receiver using `sendBulkDemo.ts` (separate `demo-*` IDs so leftover retries don't collide with the `bulk-*` IDs used in sections 1 and 3):

```bash,run
# Reset and fan out 60 deliveries against the rate-limited receiver — watch 429s
scripts/reset-receiver.sh
ts-node src/sendBulkDemo.ts 60
```

`sendBulkDemo.ts` will hang because the Activities keep retrying on every 429. After about **5 seconds**, press **Ctrl+C**.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. Only a handful of deliveries land at first; the rest get rejected with `429 Too Many Requests` and keep retrying. Open the [button label="Worker" background="#444CE7"](tab-3) tab and look for HTTP error lines ending in `429`.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities**. Most of the `demo-*` Activities should be in **Running** state with the attempt count climbing.

> **What's happening:** Temporal's per-Activity retry policy is great for one Activity that fails. It can't solve a *fleet-wide* throughput problem because the next attempt of Activity 1 fights for the same downstream slot as attempt 1 of every other Activity. The fix has to pace the dispatch itself.

---

## 3. Add the rate cap (~2 min)

Open `src/worker.ts` in the [button label="Exercise" background="#444CE7"](tab-0) tab. There's a `TODO` comment inside the `Worker.create` call. Add this option:

```typescript
maxActivitiesPerSecond: 2,
```

The Worker now dispatches at most 2 Activities per second. The full version is in the **Solution** tab.

> **Where does the excess go?** It waits in the Task Queue on the Temporal server. The Worker polls, and the server hands it work at the configured rate. Unscheduled work stays in the queue and nothing is lost.

---

## 4. Re-run with the rate cap (~3 min)

The receiver is still rate-limited at 2/sec from section 2. Dispatch at the same pace and watch the 429s vanish.

Restart the Worker so it picks up the new config. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then re-run:

```bash,run
# Restart the Worker with maxActivitiesPerSecond: 2
ts-node src/worker.ts
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send another 60:

```bash,run
# Clear leftover demo Activities, reset the receiver, then send 60 at the capped rate
scripts/stop-demo-and-reset.sh
time ts-node src/sendBulk.ts 60
```

`stop-demo-and-reset.sh` clears the receiver and stops any `demo-*` Activities still retrying from section 2.

At 2/sec, draining 60 deliveries takes about **30 seconds**. Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** and watch `bulk-*` Activities flip from **Running** to **Completed** about two per second.

The [button label="Webhook receiver" background="#444CE7"](tab-4) tab will show the `received_at` timestamps visibly spread out instead of clustering, with `processed_count` climbing by about two per second. The `throttled_count` should be a small number from the initial burst, then flat.

---

## 5. The other control: Priority (~2 min)

`maxActivitiesPerSecond` controls *how fast* dispatch happens. The companion control is **Priority**, which decides *what order* work runs in when the queue is contended: a lower `priorityKey` means higher priority, so urgent jobs can jump ahead of a backlog even when they arrive later.

We don't walk through Priority hands-on in this module, but it's worth exploring next: see [Task Queue Priority and Fairness](https://docs.temporal.io/develop/task-queue-priority-fairness) in the Temporal docs.

---

## Check your understanding

> Your downstream API has a hard rate limit of **100 req/sec**. You configure `maxActivitiesPerSecond: 10` on your Worker and deploy. Are you safe?

<details>
<summary>Answer</summary>

Safe but probably underutilizing.

10/sec is 10% of your downstream's headroom. Unless you have ~10 Workers each at 10/sec polling the same Task Queue (aggregating to 100/sec), you're leaving most of the downstream's capacity unused.

Two options:

- `maxActivitiesPerSecond` is **per Worker**. The aggregate across your Worker fleet is `N × maxActivitiesPerSecond`.
- `maxTaskQueueActivitiesPerSecond` (a `WorkerOptions` field) is **queue-wide**. It sets a hard cap regardless of Worker count. Use this when you can't predict how many Workers will be running.

</details>

---

## Coming up

**Module 05**: Heartbeats and checkpointing. Your jobs are fast and rate-capped. Next, long-running jobs report progress every few seconds and resume from the last checkpoint after a Worker crash.

---

📝 **Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
