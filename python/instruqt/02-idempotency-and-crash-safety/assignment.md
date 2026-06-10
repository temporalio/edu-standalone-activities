---
slug: idempotency-and-crash-safety
id: mowdyyydsf1e
type: challenge
title: Idempotency and crash safety
teaser: Crash the worker mid-flight; watch the receiver get two deliveries; fix it
  with one line.
notes:
- type: text
  contents: |
    # Making retries safe with idempotency

    Temporal automatically retries Activities that fail. That's almost always what you want — until the Activity has *already done something visible to the outside world* before it failed.

    A concrete example: your `deliver_webhook` Activity POSTs to the receiver's URL. The receiver gets the request and processes it. Then something errors out — the receiver returns a 500, the network drops, or the Worker crashes after the POST. Temporal sees no successful completion, so it retries the whole Activity, POST included. The receiver gets the same delivery twice.

    This is at-least-once delivery: Temporal guarantees your Activity runs to completion at least once, but doesn't guarantee exactly once. To get effectively-once *side effects*, your Activity needs to be **idempotent** — safe to run more than once with the same input. The standard way to do that is to send an idempotency key with each request and let the receiver dedupe.

    ## What you'll do

    1. Run an Activity that POSTs a webhook, then errors out on its first two attempts. Watch the Webhook receiver record 3 deliveries for one logical event.
    2. Add a one-line idempotency key to the POST. Re-run. Watch the Webhook receiver record only 1 delivery — the receiver dedupes the retries.

    The same six tabs from Module 1 are available in this module's sandbox (Exercise, Solution, Terminal, Worker, Webhook receiver, Temporal UI). There's also an **Idempotency demo** tab showing an interactive version of the diagram below.

    ## The visual version

    Step through it with the controls below, or click **Play** to watch it run. Left lane: without an idempotency key. Right lane: with one.

    <iframe src="https://raw.githack.com/temporalio/edu-standalone-activities/standalone-pre/docs/idempotency-demo/index.html" width="100%" height="560" frameborder="0" style="border: 0; border-radius: 8px;"></iframe>
tabs:
- id: 1e8ceyilglde
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- id: u62b5ywv1o86
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/solution
- id: 9q4dnwo9qhmm
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- id: kmpvpcafnhiz
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- id: jv9klqjfvbkw
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: 9stl3pgh1ly5
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
- id: s0mmzrec3kli
  title: Idempotency demo
  type: service
  hostname: workshop
  port: 9001
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Make retries safe with idempotency

Traditional job queues reimplement retry logic in every service, all behaving differently — and most punt on what to do when a job *already did something* before it failed. The card was charged. The email went out. The webhook POST landed at the receiver. Now the job errors before it can report success. The retry runs the side effect again.

Standalone Activities retry for you automatically as part of the platform — but at-least-once delivery is still at-least-once. You'll see this happen, then fix it with idempotency on the receiver side.

You'll do three things in this module:

1. Reproduce the bug: an Activity that POSTs and then fails on its first two attempts. The Webhook receiver records 3 deliveries for one logical request.
2. Add a one-line `Idempotency-Key` header derived from `activity.info().activity_id`. Re-run. The Webhook receiver now records 1 delivery.
3. Understand why the key has to be stable across retries (and not `uuid.uuid4()`).

The **Solution** tab has the finished code for this module. Peek at it whenever you want.

[button label="Try the interactive demo" background="#444CE7"](tab-6)

Estimated time: 10 minutes.

---

## 1. Reproduce the bug (~3 min)

Open `src/webhooks/activities.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. The Activity is set up to POST the webhook, then raise a retryable `ApplicationError` on its first two attempts. This simulates a transient failure that hits *after* the side effect already landed — examples: the receiver returned 500 after processing, the network dropped after the POST was accepted, the Worker crashed right after the POST.

There's a `TODO` above the `headers` dict. Leave it alone for now — you want to see what goes wrong before the fix is in place.

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send one delivery:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_standalone evt_buggy
```

(`scripts/reset-receiver.sh` clears the Webhook receiver so each run starts from a clean slate.)

The Activity fails on attempts 1 and 2, succeeds on attempt 3. Temporal's default retry policy waits a short backoff between attempts. The whole thing finishes in about 3 seconds.

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. You should see **3 deliveries** recorded for `evt_buggy` — one per attempt. The Webhook receiver tab auto-refreshes every 2 seconds, so you'll see the count climb as the retries land.

```json
{
  "count": 3,
  "deliveries": [
    { "body": { "event_id": "evt_buggy", ... }, "idempotency_key": null },
    { "body": { "event_id": "evt_buggy", ... }, "idempotency_key": null },
    { "body": { "event_id": "evt_buggy", ... }, "idempotency_key": null }
  ]
}
```

The receiver had no way to know these were duplicates of the same logical event, so it accepted all three.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and switch to the **Standalone Activities** tab in the left nav. Find `deliver-evt_buggy`. It shows the retry history — same Activity, three attempts, the last one Completed.

> **What's happening:** each attempt of the Activity body POSTed to the Webhook receiver *before* it raised. Temporal saw the error, treated it as retryable, and re-ran the Activity. The POST happened again — and again — because the Activity body is what gets replayed, not the side effect.

---

## 2. Add the idempotency key (~2 min)

Back in the [button label="Exercise" background="#444CE7"](tab-0) tab, find the `TODO` in `deliver_webhook`. Replace the empty `headers` dict with this line:

```python
headers = {"Idempotency-Key": activity.info().activity_id}
```

That's the entire fix. The full solution is in the **Solution** tab if you want to copy it.

`activity.info().activity_id` is the id you assigned when starting the Activity (`deliver-evt_buggy` here). It's **stable across retries** of the same Activity execution — every retry sees the same value. The Webhook receiver caches by this header: if it sees a key it's seen before, it returns the cached response and doesn't record a new delivery.

> **Why not `uuid.uuid4()`?** UUIDs are different every time you call them. Each retry would generate a fresh key, the receiver would see N different keys for N retries of one logical request, and your "idempotency" would dedupe nothing. The key has to be deterministic across retries.

---

## 3. Verify the fix (~3 min)

The Worker is still running the old code. Restart it so it picks up the change. In the [button label="Worker" background="#444CE7"](tab-3) tab, press **Ctrl+C**, then **Up Arrow** + **Enter** to re-run:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, send another delivery with the fix in place:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_standalone evt_fixed
```

Check the [button label="Webhook receiver" background="#444CE7"](tab-4) tab. **1 delivery** for `evt_fixed`:

```json
{
  "count": 1,
  "deliveries": [
    { "body": { "event_id": "evt_fixed", ... }, "idempotency_key": "deliver-evt_fixed" }
  ]
}
```

Three POSTs still landed at the receiver — the Activity still retried three times. But the receiver saw the same `Idempotency-Key` on each one and returned a cached response to attempts 2 and 3 without recording new deliveries.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** → find `deliver-evt_fixed`. Same retry history as the buggy run. The Activity didn't change. The receiver dedupes — that's where exactly-once *effect* lives.

> **The takeaway:** at-least-once delivery (Temporal) + idempotency (your Activity + receiver) = effectively at-most-once side effect. Temporal can't guarantee exactly-once on its own; that's a property your Activity and the system it talks to have to provide together.

---

## Check your understanding

> Your Activity body generates a random discount code (`code = random.choice(codes)`) for each delivery, and you build the `Idempotency-Key` from `code`. The Activity body works on the happy path. What goes wrong on a retry, and how do you fix it?

<details>
<summary>Answer</summary>

Each retry generates a **different** random code, so the idempotency key changes per attempt. The receiver sees N different keys for the same logical request and accepts all N. Your "idempotency" doesn't dedup anything.

The fix is to make the key deterministic across retries:

- Derive it from input fields the caller already chose (e.g. `req.event_id`), or
- Use `activity.info().activity_id`, which is stable across retries.

If you need a random code as part of the side effect, generate it **outside** the Activity (in the caller / starter) and pass it in as Activity input.

</details>

## Coming up

**Module 03** — Deduplication via ID reuse. You've made retries safe on the *receiver* side. Next: have Temporal reject duplicate submissions at the *platform* — same job ID submitted twice returns the existing handle, no second run.
