---
slug: concurrency-and-rate-limits
id: 4bet4jkgwomj
type: challenge
title: Concurrency and rate limits
teaser: Cap your Worker's throughput so a large fan-out doesn't overwhelm the downstream
  service.
notes:
- type: text
  contents: |
    # Concurrency and rate limits

    Your Activity retries safely now. By default, though, Temporal dispatches Activities as fast as the Worker can pull them off the task queue. That is often faster than the service your Activity is calling can handle.

    The downstream service (Stripe's API, the customer's webhook endpoint, your own internal queue) has a rate limit. POST faster than that limit and you get 429s back, the receiver throttles you, and your delivery latency goes up.

    The fix is one keyword argument on the Worker: max_activities_per_second. The Worker dispatches Activities at the configured pace. Everything else waits in the task queue on the server, so work is paced instead of dropped.

    ## What you'll do

    1. Run 30 deliveries with no rate cap. They all land in about a second.
    2. Switch the Webhook receiver into a "5 req/sec downstream" mode. Re-run. Watch real 429s land and Activities retry.
    3. Add max_activities_per_second=5.0 to the Worker. Re-run with the rate-limited receiver. The 429s vanish.
    4. Use the Priority parameter to push urgent deliveries ahead of bulk ones when the queue is contended.

    The same six tabs from Module 1 are available (Exercise, Solution, Terminal, Worker, Webhook receiver, Temporal UI). The **Solution** tab has the finished code if you'd rather copy than type.
tabs:
- id: itwzelvve2hv
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/04-concurrency-and-rate-limits/exercise
- id: mjbp2e2we8ey
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/04-concurrency-and-rate-limits/solution
- id: pw9qli7adehb
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/04-concurrency-and-rate-limits/exercise
- id: fnuv9fjixrx8
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/04-concurrency-and-rate-limits/exercise
- id: fax22gfly8rd
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: iwkxmfxpvx0g
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Pace your jobs and prioritize urgent work

Many job queues make rate control the consumer's problem. One busy tenant can fan out a huge batch and slow everyone else down. If the queue has no priority or rate controls, the consumer has to back off on its own or it can hammer the downstream API into 429s.

Standalone Activities give you both controls in one place: `max_activities_per_second` paces dispatch so a fan-out does not overwhelm the receiver, and `Priority` puts urgent jobs ahead of bulk ones when the queue is contended.

You'll do four things in this module:

1. Run 30 deliveries with no rate cap. They land in about a second.
2. Switch the Webhook receiver into a "5 req/sec downstream" mode. Re-run. Watch the 429s land and the Activities retry.
3. Cap the Worker at 5 dispatches per second. Re-run with the rate-limited receiver. The 429s go away.
4. Use Priority to dispatch urgent deliveries ahead of background ones.

The **Solution** tab has the finished code if you want to copy or peek. Estimated time: 12 minutes.

---

## 1. Run 30 deliveries with no rate cap (~3 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send 30 deliveries:

```bash,run
scripts/reset-receiver.sh
time uv run python -m webhooks.send_bulk 30
```

The `time` prefix prints how long the batch took. With no rate cap, the 30 deliveries should complete in **under a couple of seconds**. The Worker dispatches them as fast as the `ThreadPoolExecutor(10)` running the Activities can keep up.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. Count should be 30, and the `received_at` timestamps will all be clustered tight together (within a second or so). The Webhook receiver tab auto-refreshes every 2 seconds, so you'll see the count climb live.

Now open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and switch to the **Standalone Activities** view. You should see all 30 `bulk-*` Activities listed as **Completed**, with start and end timestamps clustered in the same one- or two-second window. Nothing in `Scheduled` state, nothing retrying. Just 30 happy deliveries at full speed.

> **What's happening:** there's no rate limit anywhere. The receiver accepted everything. In the next section you'll see what changes when the downstream actually pushes back.

---

## 2. Add a real rate limit on the receiver (~3 min)

That 5 req/sec downstream cap isn't hypothetical anymore. The Webhook receiver in this sandbox has a rate-limit mode you can switch on at runtime. When it's on, anything over the cap gets a real HTTP 429 back.

In the [button label="Terminal" background="#444CE7"](tab-2) tab, turn it on at 5 req/sec:

```bash,run
curl -fsS -X POST "http://localhost:9000/_rate_limit?limit=5"
```

Now re-send the same 30 deliveries against the rate-limited receiver:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_bulk 30
```

`send_bulk` will hang because the Activities keep retrying on every 429. After about **5 seconds**, press **Ctrl+C** in the [button label="Terminal" background="#444CE7"](tab-2) tab. You don't need to wait for it to drain; the pain is already visible.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. The state will look something like:

```json
{
  "received_count": 30,
  "processed_count": 5,
  "throttled_count": 25,
  "rate_limit": 5,
  ...
}
```

About 5 deliveries landed; about 25 got rejected with `429 Too Many Requests`. The receiver did exactly what a real rate-limited API does.

Now open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities**. You should see most of the `bulk-*` Activities in **Retrying** state with an **Attempt** count of 2 or 3, and a `lastFailure` of `HTTPStatusError: 429`. Temporal is doing the right thing for each Activity in isolation: retry on transient failure. But there's nothing slowing the *dispatch* down, so the receiver keeps getting hammered.

> **What's happening:** Temporal's per-Activity retry policy is great for one Activity that fails. It can't solve a *whole-fleet* throughput problem because the next attempt of attempt 1 fights for the same downstream slot as attempt 1 of every other Activity. The fix has to pace the dispatch itself, not retry harder.

---

## 3. Add the rate cap (~2 min)

Open `src/webhooks/worker.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. There's a `TODO` inside the `Worker(...)` constructor. Add this line right after the `activity_executor` argument:

```python
max_activities_per_second=5.0,
```

The Worker now dispatches at most 5 Activities per second. The full version is in the **Solution** tab.

> **Where does the excess go?** It waits in the task queue on the Temporal server. The Worker polls, and the server hands it work at the configured rate. Unscheduled work stays in the queue. Nothing is lost or dropped.

---

## 4. Re-run with the rate cap (~3 min)

The receiver is still rate-limited at 5/sec from Section 2. We're going to dispatch at the same pace from the Worker and watch the 429s vanish.

Restart the Worker so it picks up the new config. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then **Up Arrow + Enter**:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send another 30:

```bash,run
scripts/reset-receiver.sh
time uv run python -m webhooks.send_bulk 30
```

This time the wall-clock time is noticeably longer, typically **4 to 6 seconds** for 30 deliveries at 5/sec. The rate limiter on the Worker side allows a small initial burst, then enforces the cap. In the [button label="Webhook receiver" background="#444CE7"](tab-4) tab the `received_at` timestamps will visibly spread out instead of clustering.

Check the receiver state at the end:

```json
{
  "received_count": 30,
  "processed_count": 30,
  "throttled_count": 0,
  "rate_limit": 5,
  ...
}
```

**Zero throttled.** The receiver is still capped at 5/sec, but the Worker never asked it for more than that. Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** and you'll see Activities completing cleanly with `Attempt: 1`. No retries, no failures.

> **What's happening:** same 30 units of work, same delivery outcome. The Worker just dispatched them at a steady pace instead of all at once. Pair `max_activities_per_second` with `max_concurrent_activities` (which caps how many run in parallel) when you need both dispatch-rate and in-flight-count controls.

---

## 5. Priority: send urgent work ahead of bulk (~3 min)

`max_activities_per_second` controls *how fast* dispatch happens. `Priority` controls *in what order* when the queue is contended. Lower `priority_key` means higher priority, so `priority_key=1` runs before `priority_key=5`.

There's a demo script in the exercise that does this for you. In the [button label="Terminal" background="#444CE7"](tab-2) tab:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_priority_demo
```

The script submits 10 background deliveries (`priority_key=5`) and then 3 urgent ones (`priority_key=1`). The Worker is rate-capped at 5/sec, so the queue is contended. That is the situation where priority matters.

Open the [button label="Webhook receiver" background="#444CE7"](tab-4) tab and look at the `received_at` order. The `urgent_*` deliveries land ahead of most of the `bg_*` ones, even though they were submitted later.

For **multi-tenant fairness**, where you want busy tenants to avoid starving quieter ones, pass `fairness_key=<tenant_id>` and `fairness_weight=<float>` on the same `Priority(...)` object. A deeper multi-tenant example belongs in a future module.

> **What's happening:** `Priority` is metadata Temporal's server uses when deciding which task to dispatch next. Combined with the rate cap from Section 3, you control both how fast work runs and which work runs first.

---

## Check your understanding

> Your downstream API has a hard rate limit of **100 req/sec**. You configure `max_activities_per_second=10` on your Worker and deploy. Are you safe?

<details>
<summary>Answer</summary>

Safe but **probably underutilizing**.

10/sec is 10% of your downstream's headroom. Unless you have ~10 Workers each at 10/sec polling the same Task Queue (aggregating to 100/sec), you're leaving most of the downstream's capacity unused.

Two knobs to remember:

- `max_activities_per_second` is **per Worker**. If N Workers are polling the same Task Queue, the aggregate is `N × max_activities_per_second`. Tune by dividing your safe rate budget across the Worker fleet.
- `max_task_queue_activities_per_second` is **queue-wide**. It sets a hard cap regardless of Worker count. Use this when you can't predict how many Workers will be running.

</details>

---

## Coming up

**Module 05**: Heartbeats and checkpointing. Your jobs are fast, fair, and rate-capped. Next, long-running jobs report progress every few seconds and resume from the last checkpoint after a Worker crash.
