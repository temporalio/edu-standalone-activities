---
slug: concurrency-and-rate-limits
id: bf671ejc9oen
type: challenge
title: Concurrency and rate limits
teaser: Cap your worker's throughput so a runaway fan-out doesn't DDoS the downstream
  service.
notes:
- type: text
  contents: |
    # Concurrency and rate limits

    Your activity is safe to retry now. Time to make sure it doesn't
    overwhelm whatever it's calling.

    The downstream service (Stripe's API, your customer's webhook
    endpoint, your own internal queue) almost certainly has a rate
    limit. A naive fan-out blows through that limit and gets you 429s,
    SLO violations, and unhappy partners.

    The fix is one keyword argument: `max_activities_per_second` on
    the Worker. Temporal holds the excess work durably in the task
    queue while the worker dispatches at the configured pace. No work
    is lost; throughput is shaped.

    You'll fan-out 30 deliveries twice — first without a cap (all hit
    in ~1 second) and then with a cap (smoothly spread over ≥6
    seconds).
tabs:
- id: hvzxgvjkgch5
  title: Editor
  type: code
  hostname: workshop
  path: /root/workshop/exercises/03-concurrency-and-rate-limits/exercise
- id: 4tfuoxsgkh61
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/03-concurrency-and-rate-limits/solution
- id: uye7c8clfvle
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/03-concurrency-and-rate-limits/exercise
- id: 9qmbudhyhgex
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/03-concurrency-and-rate-limits/exercise
- id: daugc4aefzka
  title: Echo server
  type: service
  hostname: workshop
  port: 9000
- id: rvmmd1xpmkyf
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 900
enhanced_loading: null
---

# Concurrency and rate limits

Your activity is safe to retry. Now stop it from DDoSing the downstream.

By the end you'll have:

- Seen 30 webhook deliveries flood the receiver in under a second.
- Capped the worker's dispatch rate with `max_activities_per_second=5.0`.
- Watched the same 30 deliveries spread evenly over ≥6 seconds while Temporal holds the excess durably in the task queue.

Budget ~10 minutes.

---

## 1. Fan-out without a cap (~3 min)

In the [button label="Worker" background="#444CE7"](tab-2) tab:

```bash
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-1) tab:

```bash
scripts/reset-echo.sh
time uv run python -m webhooks.send_bulk 30
```

That `time` prefix prints how long the whole batch took. With no rate cap, all 30 deliveries should complete in **under a couple of seconds** — the worker fires them off as fast as `ThreadPoolExecutor(10)` allows.

Confirm in the [button label="Echo server" background="#444CE7"](tab-3) tab — count should be 30, and the `received_at` timestamps will all be clustered tight.

> **What just happened?** No rate limit anywhere. The worker dispatched activities as fast as it could pull them from the task queue. If the echo server were a real downstream API with a 5 req/sec limit, this run would have triggered 25 rate-limit errors.

---

## 2. Add the cap (~2 min)

Open `src/webhooks/worker.py` in the [button label="Editor" background="#444CE7"](tab-0) tab. Find the TODO inside the `Worker(...)` constructor and add the kwarg:

```python
max_activities_per_second=5.0,
```

The Worker now dispatches at most 5 activities per second.

> **Where does the excess go?** Temporal's task queue, on the server. The worker polls; the server hands it work at the worker's configured rate. Unscheduled work sits durably in the queue. Nothing is lost or dropped.

---

## 3. Fan-out with the cap (~3 min)

Restart the worker so it picks up the new config. In the [button label="Worker" background="#444CE7"](tab-2) tab:

```
# Ctrl+C to kill the worker, then:
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-1) tab:

```bash
scripts/reset-echo.sh
time uv run python -m webhooks.send_bulk 30
```

The wall-clock time should now be **at least 6 seconds** — 30 deliveries ÷ 5/sec = 6s. The [button label="Echo server" background="#444CE7"](tab-3) tab's `received_at` timestamps will visibly spread out.

Open the [button label="Temporal UI" background="#444CE7"](tab-4) tab while the batch runs and watch the activity list. You'll see some activities in `Scheduled` state — the server holding them durably while the worker dispatches at 5/sec.

> **What just happened?** Same 30 units of work, same outcome. But the dispatch shape changed: a thundering herd became a steady stream. Pair `max_activities_per_second` with `max_concurrent_activities` (which caps *in-flight* count) for full-spectrum downstream protection.

---

## 4. Priority: label which work matters (~3 min)

Rate cap controls *how fast*. Priority controls *in what order* when the queue is contended. Lower `priority_key` = higher priority.

A demo script is provided. In the [button label="Terminal" background="#444CE7"](tab-1) tab:

```bash
scripts/reset-echo.sh
uv run python -m webhooks.send_priority_demo
```

This fires 10 background deliveries (`priority_key=5`) followed by 3 urgent ones (`priority_key=1`). The worker is rate-capped at 5/sec, so the queue is contended — exactly the situation where priority matters.

Open the [button label="Echo server" background="#444CE7"](tab-3) tab and look at the `received_at` order of the `urgent_*` deliveries relative to the `bg_*` ones. The urgent batch jumps the queue.

For multi-tenant fairness — where you want "loud" tenants to not starve "quiet" ones rather than always-jump-the-queue priority — pass `fairness_key=<tenant_id>` and `fairness_weight=<float>` on the same `Priority(...)` object. A full multi-tenant demo lives in a future module.

> **What just happened?** You labeled work with first-class metadata that Temporal's server uses to shape dispatch order, not just rate. Pair it with the rate cap from Section 3 and you've got both throughput shaping and importance shaping.

---

## Check your understanding

> Your downstream API has a hard rate limit of **100 req/sec**. You configure `max_activities_per_second=10` on your worker and deploy. Are you safe?

<details>
<summary>Answer</summary>

Safe but **probably underutilizing**.

10/sec is 10% of your downstream's headroom. Unless you have ~10 workers each at 10/sec polling the same task queue (aggregating to 100/sec), you're leaving most of the downstream's capacity unused.

Two knobs to remember:

- `max_activities_per_second` is **per worker**. If N workers are polling the same task queue, the aggregate is `N × max_activities_per_second`. Tune by dividing your safe rate budget across the worker fleet.
- `max_task_queue_activities_per_second` is **queue-wide** — a hard cap regardless of worker count. Use this when you can't predict how many workers will be running.

</details>

---

## Check

Press **Check** when 30 bulk deliveries arrive at the echo server in ≥6 seconds of wall-clock time.

---

## Coming up

**Module 04** — Dedup via ID reuse. Module 02 made each delivery safe under retry. Module 04 makes each delivery happen at most once even when the upstream system sends the same event twice.
