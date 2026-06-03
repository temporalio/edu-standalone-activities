---
slug: idempotency-and-crash-safety
id: ke3maxyqbkyf
type: challenge
title: Idempotency and crash safety
teaser: Crash the worker mid-flight; watch the receiver get two deliveries; fix it
  with one line.
notes:
- type: text
  contents: |
    # Idempotency and crash safety

    Temporal gives you **at-least-once** delivery, not exactly-once. If
    your worker crashes after an activity's side effect succeeded but
    before Temporal hears about it, Temporal retries — and the side
    effect happens twice.

    In this module you'll:

    1. Reproduce the bug — kill a worker mid-flight, watch the echo
       server receive the same webhook twice.
    2. Fix it with one line — an `Idempotency-Key` header from
       `activity.info().activity_id` (stable across retries) that lets
       the receiver dedup.

    The fix is small. The intuition it builds is large.
tabs:
- id: mixqrvicyeey
  title: Editor
  type: code
  hostname: workshop
  path: /root/workshop
- id: ilosldpsh5zx
  title: Terminal
  type: terminal
  hostname: workshop
- id: tl58vxopklzz
  title: Worker
  type: terminal
  hostname: workshop
- id: 1skz9ini3o8j
  title: Echo server
  type: service
  hostname: workshop
  port: 9000
- id: g61qtex8phlz
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 900
enhanced_loading: null
---

# Idempotency and crash safety

Temporal retries activities that don't complete cleanly. Most of the time that's exactly what you want. But when an activity has already produced a side effect (a webhook, a charge, an email) and *then* the worker dies, the retry runs the side effect again — unless your activity is idempotent.

By the end you'll have:

- Reproduced a double-delivery by crashing a worker mid-flight.
- Fixed it with a single line — an HTTP `Idempotency-Key` header that the receiver dedupes on.
- Understood why the key has to come from `activity.info().activity_id` and not `uuid4()`.

Budget ~10 minutes.

---

## 1. See the bug (~3 min)

Open `src/webhooks/activities.py` in the [button label="Editor" background="#444CE7"](tab-0) tab. The activity now POSTs the webhook and then sleeps for 4 seconds — simulating the (real) window between "side effect succeeded" and "Temporal got the ack." There's a TODO above the headers dict; **leave it alone for now** so you can see what goes wrong without the fix.

In the [button label="Worker" background="#444CE7"](tab-2) tab:

```bash
cd /root/workshop
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-1) tab:

```bash
cd /root/workshop
scripts/reset-echo.sh
uv run python -m webhooks.send_standalone evt_buggy
```

The activity is now running. The POST already hit the echo server. The worker is in the 4-second sleep window. Quick — kill it before it returns:

```bash
scripts/kill-worker.sh
scripts/restart-worker.sh
```

Open the [button label="Echo server" background="#444CE7"](tab-3) tab and look at the `count` field. **2 deliveries** for `evt_buggy` — once from the original attempt that crashed, once from the retry after the restart.

> **What just happened?** The POST already succeeded. The sleep was meant to simulate the gap before Temporal heard "complete." You killed the worker during that gap, so Temporal never got the ack — it dispatched a retry. The retried activity ran top-to-bottom again and POSTed a second time. The receiver had no way to know the second POST was a duplicate, so it accepted both.

---

## 2. Add the fix (~2 min)

Back in the [button label="Editor" background="#444CE7"](tab-0) tab, find the TODO in `deliver_webhook`. Replace the empty headers dict with one that includes an `Idempotency-Key`:

```python
headers = {"Idempotency-Key": activity.info().activity_id}
```

> **Why `activity.info().activity_id`?** It's *stable across retries* of the same activity execution. Every retry sees the same value, so the receiver can recognize the duplicate. If you used `uuid.uuid4()` here, each retry would generate a fresh value — defeating the whole point.

---

## 3. Verify the fix (~3 min)

Now repeat the chaos sequence with the fix in place:

```bash
scripts/reset-echo.sh
scripts/kill-worker.sh
scripts/restart-worker.sh
uv run python -m webhooks.send_standalone evt_fixed
```

Quickly:

```bash
scripts/kill-worker.sh
scripts/restart-worker.sh
```

Check the [button label="Echo server" background="#444CE7"](tab-3) tab. **1 delivery** for `evt_fixed` — and one of the records will have `"deduped": true` in its server response, signalling that the receiver saw a duplicate key and refused to re-process.

Open the [button label="Temporal UI" background="#444CE7"](tab-4) tab and find the `deliver-evt_fixed` activity. Look at the **Attempt** number — it should be 2 or higher. That's the receipt: Temporal *did* retry, but the receiver's idempotency check absorbed the duplicate.

> **What just happened?** The first POST was logged in the receiver's cache by its idempotency key. When the retry POSTed with the same key, the receiver returned the cached response without recording a new delivery. At-least-once delivery + idempotency = effectively at-most-once side effect.

---

## Check

Press **Check** when the echo server shows exactly **1** delivery for `evt_fixed` and the Temporal UI shows attempt ≥ 2 for `deliver-evt_fixed`.

---

## Coming up

**Module 03** — Concurrency, rate limits, and priority. You've made each delivery safe. Next: cap how many of them fire at once so you don't DDoS your own customers.
