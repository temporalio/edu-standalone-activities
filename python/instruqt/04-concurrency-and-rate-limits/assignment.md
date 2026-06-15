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

    1. Run 60 deliveries with no rate cap. They all land in about a second.
    2. Switch the Webhook receiver into a "2 req/sec downstream" mode. Re-run. Watch real 429s land and Activities retry.
    3. Add max_activities_per_second=2.0 to the Worker. Re-run with the rate-limited receiver. The flood of 429s stops.
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

1. Run 60 deliveries with no rate cap. They land in about a second.
2. Switch the Webhook receiver into a "2 req/sec downstream" mode. Re-run. Watch the 429s land and the Activities retry.
3. Cap the Worker at 2 dispatches per second. Re-run with the rate-limited receiver. The flood of 429s stops.
4. Use Priority to dispatch urgent deliveries ahead of background ones.

The **Solution** tab has the finished code if you want to copy or peek. Estimated time: 12 minutes.

---

## 1. Run 60 deliveries with no rate cap (~3 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send 60 deliveries:

```bash,run
scripts/reset-receiver.sh
time uv run python -m webhooks.send_bulk 60
```

The `time` prefix prints how long the batch took. With no rate cap, the 60 deliveries should complete in **a second or two**. The Worker dispatches them as fast as the `ThreadPoolExecutor(10)` running the Activities can keep up.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. `received_count` and `processed_count` should both reach 60, and the `received_at` timestamps will all be clustered tight together (within a second or so). The Webhook receiver tab auto-refreshes every 2 seconds, so you'll see `received_count` climb live.

Now open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and switch to the **Standalone Activities** view. You should see all 60 `bulk-*` Activities listed as **Completed**, with start and end timestamps clustered in the same one- or two-second window. Nothing in `Scheduled` state, nothing retrying. Just 60 happy deliveries at full speed.

> **What's happening:** there's no rate limit anywhere. The receiver accepted everything. In the next section you'll see what changes when the downstream actually pushes back.

---

## 2. Add a real rate limit on the receiver (~3 min)

That 2 req/sec downstream cap isn't hypothetical anymore. The Webhook receiver in this sandbox has a rate-limit mode you can switch on at runtime. When it's on, anything over the cap gets a real HTTP 429 back.

In the [button label="Terminal" background="#444CE7"](tab-2) tab, turn it on at 2 req/sec:

```bash,run
curl -fsS -X POST "http://localhost:9000/_rate_limit?limit=2"
```

Now re-send 60 deliveries against the rate-limited receiver using `send_bulk_demo` (`src/webhooks/send_bulk_demo.py`), a sibling of `send_bulk` that uses `demo-*` Activity IDs so its in-flight retries do not collide with the `bulk-*` IDs Sections 3 and 4 reuse:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_bulk_demo 60
```

`send_bulk_demo` will hang because the Activities keep retrying on every 429. While it is running, quickly open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities**. Running `demo-*` Activities only stay easy to catch for a few seconds, so re-run the command if you miss them.

After about **5 seconds**, go back to the [button label="Terminal" background="#444CE7"](tab-2) tab and press **Ctrl+C**. You don't need to wait for it to drain; the pain is already visible.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. The state will look something like:

```json
{
  "received_count": "increasing (retries)",
  "processed_count": 2,
  "throttled_count": "increasing (retries)",
  "rate_limit": 2,
  ...
}
```

Only a handful of deliveries land at first; the rest get rejected with `429 Too Many Requests` and keep retrying. The receiver did exactly what a real rate-limited API does. To see the actual failures, switch to the [button label="Worker" background="#444CE7"](tab-3) tab and look for HTTP request log lines ending in `HTTP/1.0 429 Too Many Requests`.

In that same Temporal UI view, you should see most of the `demo-*` Activities in **Running** state with an **Attempt** count of 2 or 3, and a `lastFailure` of `HTTPStatusError: 429`. Temporal is doing the right thing for each Activity in isolation: retry on transient failure. But there's nothing slowing the *dispatch* down, so the receiver keeps getting hammered.

> **What's happening:** Temporal's per-Activity retry policy is great for one Activity that fails. It can't solve a *whole-fleet* throughput problem because the next attempt of attempt 1 fights for the same downstream slot as attempt 1 of every other Activity. The fix has to pace the dispatch itself, not retry harder.

---

## 3. Add the rate cap (~2 min)

Open `src/webhooks/worker.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. There's a `TODO` inside the `Worker(...)` constructor. Add this line right after the `activity_executor` argument:

```python
max_activities_per_second=2.0,
```

The Worker now dispatches at most 2 Activities per second. The full version is in the **Solution** tab.

> **Where does the excess go?** It waits in the Task Queue on the Temporal server. The Worker polls, and the server hands it work at the configured rate. Unscheduled work stays in the queue. Nothing is lost or dropped.

---

## 4. Re-run with the rate cap (~3 min)

The receiver is still rate-limited at 2/sec from Section 2. We're going to dispatch at the same pace from the Worker and watch the 429s vanish.

Restart the Worker so it picks up the new config. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then **Up Arrow + Enter**:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send another 60:

```bash,run
scripts/reset-receiver.sh
time uv run python -m webhooks.send_bulk 60
```

Right after you press **Enter**, jump to the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities**. At 2/sec, draining 60 deliveries takes about **half a minute**, so you have a comfortable window to watch it happen live: refresh the view and you'll see `bulk-*` Activities sitting in **Scheduled**, a couple flipping to **Running**, then **Completed**, roughly two per second, while the rest wait their turn. No racing the clock or reading timestamps after the fact — the pacing is visible as it unfolds.

This time the wall-clock time is noticeably longer, around **30 seconds** for 60 deliveries at 2/sec. The rate limiter on the Worker side allows a small initial burst, then enforces the cap. In the [button label="Webhook receiver" background="#444CE7"](tab-4) tab the `received_at` timestamps will visibly spread out instead of clustering, with `received_count` climbing by about two per second.

Check the receiver state at the end:

```json
{
  "received_count": 65,
  "processed_count": 60,
  "throttled_count": 5,
  "rate_limit": 2,
  ...
}
```

**Compare that to Section 2.** There `throttled_count` climbed continuously and never settled, because dispatch never slowed down. Here `throttled_count` shows only a small handful — these come from the Worker's initial burst, which briefly outpaces the receiver before the cap kicks in (the exact number varies run to run, and can be zero). After that it stops climbing: all 60 are delivered (`processed_count: 60`) and the receiver is no longer being hammered. You can confirm the pattern in the [button label="Worker" background="#444CE7"](tab-3) tab: a few early `HTTP/1.0 429 Too Many Requests` lines, then a steady stream of `HTTP/1.0 200 OK`. Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** and you'll see `bulk-*` Activities completing instead of piling up in retries.

> **What's happening:** same 60 units of work, same delivery outcome. The Worker just dispatched them at a steady pace instead of all at once. To eliminate even the startup burst, cap the Worker a little below the downstream limit. Pair `max_activities_per_second` with `max_concurrent_activities` (which caps how many run in parallel) when you need both dispatch-rate and in-flight-count controls.

---

## 5. Priority: send urgent work ahead of bulk (~3 min)

`max_activities_per_second` controls *how fast* dispatch happens. `Priority` controls *in what order* when the queue is contended. Lower `priority_key` means higher priority, so `priority_key=1` runs before `priority_key=5`.

There's a demo script in the exercise that does this for you. In the [button label="Terminal" background="#444CE7"](tab-2) tab:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_priority_demo
```

The script submits 20 background deliveries (`priority_key=5`) and then 5 urgent ones (`priority_key=1`). The Worker is rate-capped at 2/sec, so the queue is contended. That is the situation where priority matters.

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
