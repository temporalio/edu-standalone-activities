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
  path: /root/workshop
- id: uye7c8clfvle
  title: Terminal
  type: terminal
  hostname: workshop
- id: 9qmbudhyhgex
  title: Worker
  type: terminal
  hostname: workshop
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
cd /root/workshop
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-1) tab:

```bash
cd /root/workshop
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

## Going further: Priority and Fairness

`Priority(priority_key=..., fairness_key=..., fairness_weight=...)` on `client.start_activity` lets you express *which* tenant's work should jump the queue when capacity is tight. A future module covers it in depth; for now, know it exists and pairs naturally with the rate cap.

---

## Check

Press **Check** when 30 bulk deliveries arrive at the echo server in ≥6 seconds of wall-clock time.

---

## Coming up

**Module 04** — Dedup via ID reuse. Module 02 made each delivery safe under retry. Module 04 makes each delivery happen at most once even when the upstream system sends the same event twice.
