---
slug: idempotency-and-crash-safety
id: ke3maxyqbkyf
type: challenge
title: Idempotency and crash safety
teaser: Crash the worker mid-flight; watch the receiver get two deliveries; fix it
  with one line.
notes:
- type: text
  contents: "# Idempotency and crash safety\n\nTemporal gives you **at-least-once**
    delivery, not exactly-once. If\nyour worker crashes after an activity's side effect
    succeeded but\nbefore Temporal hears about it, Temporal retries — and the side\neffect
    happens twice.\n\nIn this module you'll:\n\n1. Reproduce the bug — kill a worker
    mid-flight, watch the echo\n   server receive the same webhook twice.\n2. Fix
    it with one line — an `Idempotency-Key` header from\n   `activity.info().activity_id`
    (stable across retries) that lets\n   the receiver dedup.\n\nThe fix is small.
    The intuition it builds is large.\n\n<svg xmlns=\"http://www.w3.org/2000/svg\"
    viewBox=\"0 0 1100 460\" font-family=\"system-ui, -apple-system, 'Segoe UI', sans-serif\">\n
    \ <rect width=\"1100\" height=\"460\" fill=\"#1a1a2e\"/>\n  <text x=\"550\" y=\"28\"
    text-anchor=\"middle\" fill=\"#e2e8f0\" font-size=\"18\" font-weight=\"600\">Same
    chaos sequence, two outcomes</text>\n  <text x=\"550\" y=\"48\" text-anchor=\"middle\"
    fill=\"#a0aec0\" font-size=\"12\">Worker crashes mid-flight. Temporal retries.
    Watch what the receiver does.</text>\n  <g transform=\"translate(20, 70)\">\n
    \   <rect width=\"510\" height=\"370\" fill=\"none\" stroke=\"#e53e3e\" stroke-width=\"1.5\"
    stroke-dasharray=\"4 3\" rx=\"6\"/>\n    <text x=\"255\" y=\"22\" text-anchor=\"middle\"
    fill=\"#fc8181\" font-size=\"14\" font-weight=\"600\">Without Idempotency-Key</text>\n
    \   <rect x=\"180\" y=\"40\" width=\"150\" height=\"44\" fill=\"#2d3748\" stroke=\"#4a5568\"
    rx=\"4\"/>\n    <text x=\"255\" y=\"60\" text-anchor=\"middle\" fill=\"#e2e8f0\"
    font-size=\"12\" font-weight=\"600\">Client</text>\n    <text x=\"255\" y=\"76\"
    text-anchor=\"middle\" fill=\"#a0aec0\" font-size=\"10\" font-family=\"ui-monospace,
    monospace\">POST /hooks</text>\n    <text x=\"255\" y=\"115\" text-anchor=\"middle\"
    fill=\"#fc8181\" font-size=\"32\" font-weight=\"700\" opacity=\"0\">\n      \U0001F4A5\n
    \     <animate attributeName=\"opacity\" values=\"0;0;1;1;0;0;0\" keyTimes=\"0;0.30;0.32;0.42;0.45;0.99;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n    </text>\n    <line x1=\"255\" y1=\"90\"
    x2=\"255\" y2=\"265\" stroke=\"#4a5568\" stroke-width=\"1.2\" stroke-dasharray=\"3
    4\"/>\n    <rect x=\"170\" y=\"270\" width=\"170\" height=\"80\" fill=\"#2d3748\"
    stroke=\"#4a5568\" rx=\"4\"/>\n    <text x=\"255\" y=\"292\" text-anchor=\"middle\"
    fill=\"#e2e8f0\" font-size=\"12\" font-weight=\"600\">Echo Server</text>\n    <text
    x=\"255\" y=\"312\" text-anchor=\"middle\" fill=\"#a0aec0\" font-size=\"10\">deliveries</text>\n
    \   <text x=\"255\" y=\"338\" text-anchor=\"middle\" fill=\"#fc8181\" font-size=\"24\"
    font-weight=\"700\">\n      0\n      <animate attributeName=\"opacity\" values=\"1;1;0;0;0;0;0\"
    keyTimes=\"0;0.18;0.20;0.55;0.60;0.99;1\" dur=\"10s\" repeatCount=\"indefinite\"/>\n
    \   </text>\n    <text x=\"255\" y=\"338\" text-anchor=\"middle\" fill=\"#f6e05e\"
    font-size=\"24\" font-weight=\"700\" opacity=\"0\">\n      1\n      <animate attributeName=\"opacity\"
    values=\"0;0;1;1;0;0;0\" keyTimes=\"0;0.18;0.20;0.55;0.60;0.99;1\" dur=\"10s\"
    repeatCount=\"indefinite\"/>\n    </text>\n    <text x=\"255\" y=\"338\" text-anchor=\"middle\"
    fill=\"#fc8181\" font-size=\"24\" font-weight=\"700\" opacity=\"0\">\n      2\n
    \     <animate attributeName=\"opacity\" values=\"0;0;0;0;1;1;0\" keyTimes=\"0;0.55;0.58;0.60;0.62;0.99;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n    </text>\n    <circle r=\"7\" fill=\"#f6e05e\"
    stroke=\"#1a1a2e\" stroke-width=\"1.5\" opacity=\"0\">\n      <animate attributeName=\"cy\"
    values=\"62;62;320;320;62;62;320;320\" keyTimes=\"0;0.02;0.18;0.30;0.45;0.47;0.58;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n      <animate attributeName=\"cx\"
    values=\"255\" dur=\"10s\" repeatCount=\"indefinite\"/>\n      <animate attributeName=\"opacity\"
    values=\"0;1;1;0;0;1;1;0\" keyTimes=\"0;0.02;0.18;0.20;0.45;0.47;0.58;0.60\" dur=\"10s\"
    repeatCount=\"indefinite\"/>\n    </circle>\n  </g>\n  <g transform=\"translate(570,
    70)\">\n    <rect width=\"510\" height=\"370\" fill=\"none\" stroke=\"#38a169\"
    stroke-width=\"1.5\" stroke-dasharray=\"4 3\" rx=\"6\"/>\n    <text x=\"255\"
    y=\"22\" text-anchor=\"middle\" fill=\"#9ae6b4\" font-size=\"14\" font-weight=\"600\">With
    Idempotency-Key</text>\n    <rect x=\"180\" y=\"40\" width=\"150\" height=\"44\"
    fill=\"#2d3748\" stroke=\"#4a5568\" rx=\"4\"/>\n    <text x=\"255\" y=\"60\" text-anchor=\"middle\"
    fill=\"#e2e8f0\" font-size=\"12\" font-weight=\"600\">Client</text>\n    <text
    x=\"255\" y=\"76\" text-anchor=\"middle\" fill=\"#a0aec0\" font-size=\"10\" font-family=\"ui-monospace,
    monospace\">POST /hooks (key=ABC)</text>\n    <text x=\"255\" y=\"115\" text-anchor=\"middle\"
    fill=\"#fc8181\" font-size=\"32\" font-weight=\"700\" opacity=\"0\">\n      \U0001F4A5\n
    \     <animate attributeName=\"opacity\" values=\"0;0;1;1;0;0;0\" keyTimes=\"0;0.30;0.32;0.42;0.45;0.99;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n    </text>\n    <line x1=\"255\" y1=\"90\"
    x2=\"255\" y2=\"265\" stroke=\"#4a5568\" stroke-width=\"1.2\" stroke-dasharray=\"3
    4\"/>\n    <text x=\"255\" y=\"195\" text-anchor=\"middle\" fill=\"#9ae6b4\" font-size=\"11\"
    font-weight=\"600\" opacity=\"0\">\n      server sees ABC again → deduped\n      <animate
    attributeName=\"opacity\" values=\"0;0;0;0;1;1;0\" keyTimes=\"0;0.55;0.58;0.60;0.62;0.99;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n    </text>\n    <rect x=\"170\" y=\"270\"
    width=\"170\" height=\"80\" fill=\"#2d3748\" stroke=\"#4a5568\" rx=\"4\"/>\n    <text
    x=\"255\" y=\"292\" text-anchor=\"middle\" fill=\"#e2e8f0\" font-size=\"12\" font-weight=\"600\">Echo
    Server</text>\n    <text x=\"255\" y=\"312\" text-anchor=\"middle\" fill=\"#a0aec0\"
    font-size=\"10\">deliveries</text>\n    <text x=\"255\" y=\"338\" text-anchor=\"middle\"
    fill=\"#9ae6b4\" font-size=\"24\" font-weight=\"700\">\n      0\n      <animate
    attributeName=\"opacity\" values=\"1;1;0;0;0;0;0\" keyTimes=\"0;0.18;0.20;0.55;0.60;0.99;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n    </text>\n    <text x=\"255\" y=\"338\"
    text-anchor=\"middle\" fill=\"#9ae6b4\" font-size=\"24\" font-weight=\"700\" opacity=\"0\">\n
    \     1\n      <animate attributeName=\"opacity\" values=\"0;0;1;1;1;1;0\" keyTimes=\"0;0.18;0.20;0.55;0.60;0.99;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n    </text>\n    <circle r=\"7\" fill=\"#f6e05e\"
    stroke=\"#1a1a2e\" stroke-width=\"1.5\" opacity=\"0\">\n      <animate attributeName=\"cy\"
    values=\"62;62;320;320;62;62;320;320\" keyTimes=\"0;0.02;0.18;0.30;0.45;0.47;0.58;1\"
    dur=\"10s\" repeatCount=\"indefinite\"/>\n      <animate attributeName=\"cx\"
    values=\"255\" dur=\"10s\" repeatCount=\"indefinite\"/>\n      <animate attributeName=\"opacity\"
    values=\"0;1;1;0;0;1;1;0\" keyTimes=\"0;0.02;0.18;0.20;0.45;0.47;0.58;0.60\" dur=\"10s\"
    repeatCount=\"indefinite\"/>\n    </circle>\n  </g>\n  <text x=\"550\" y=\"455\"
    text-anchor=\"middle\" fill=\"#cbd5e0\" font-size=\"12\">Same crash, same retry.
    The Idempotency-Key header is the only difference.</text>\n</svg>\n"
tabs:
- id: mixqrvicyeey
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- id: nry5elwiiegg
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/02-idempotency-and-crash-safety/solution
- id: ilosldpsh5zx
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
- id: tl58vxopklzz
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/02-idempotency-and-crash-safety/exercise
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
- id: fcrl1fl2kc64
  title: Idempotency demo
  type: service
  hostname: workshop
  port: 9001
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Idempotency and crash safety

Temporal retries activities that don't complete cleanly. Most of the time that's exactly what you want. But when an activity has already produced a side effect (a webhook, a charge, an email) and *then* the worker dies, the retry runs the side effect again — unless your activity is idempotent.

By the end you'll have:

- Reproduced a double-delivery by crashing a worker mid-flight.
- Fixed it with a single line — an HTTP `Idempotency-Key` header that the receiver dedupes on.
- Understood why the key has to come from `activity.info().activity_id` and not `uuid4()`.

Budget ~10 minutes.

[button label="Interactive Demo: Try Me" background="#444CE7"](tab-6)

---

## 1. See the bug (~3 min)

Open `src/webhooks/activities.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. The activity POSTs the webhook and then sleeps for 15 seconds — simulating the (real) window between "side effect succeeded" and "Temporal got the ack." There's a TODO above the headers dict; **leave it alone for now** so you can see what goes wrong without the fix.

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the worker:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, schedule one delivery:

```bash,run
scripts/reset-echo.sh
uv run python -m webhooks.send_standalone evt_buggy
```

`send_standalone` uses `client.start_activity`, which enqueues the work durably on the server and returns immediately. The terminal is free again, but on the worker the POST has already hit the echo server and the worker is now in the 15-second sleep window.

Quickly — crash the worker mid-sleep:

```bash,run
scripts/kill-worker.sh
```

(SIGKILL, not the default SIGTERM. A graceful SIGTERM would let the sleep finish, the activity complete cleanly, and no retry would fire.)

Switch to the [button label="Worker" background="#444CE7"](tab-3) tab — the worker process has died. Press **Up Arrow + Enter** to restart it:

```bash,run
uv run python -m webhooks.worker
```

Temporal noticed the orphaned attempt never acked, but it has to wait for the `start_to_close_timeout` (20s on this activity) to fire before declaring the attempt timed out and dispatching a retry. **Wait ~20 seconds**, then check the [button label="Echo server" background="#444CE7"](tab-4) tab. **2 deliveries** for `evt_buggy` — one from the crashed attempt, one from the retry.

> **Where is this in the Temporal UI?** Standalone activities don't show in the **Workflows** view — that view is for Workflow Executions, of which we have none in this module. Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab and look for an **Activities** view (left nav). You should find `deliver-evt_buggy` with **attempt = 2**.

> **What just happened?** The POST already succeeded. The sleep was meant to simulate the gap before Temporal heard "complete." You killed the worker during that gap, so Temporal eventually timed out the attempt and dispatched a retry. The retried activity ran top-to-bottom again and POSTed a second time. The receiver had no way to know the second POST was a duplicate, so it accepted both.

---

## 2. Add the fix (~2 min)

Back in the [button label="Exercise" background="#444CE7"](tab-0) tab, find the TODO in `deliver_webhook`. Replace the empty headers dict with one that includes an `Idempotency-Key`:

```python
headers = {"Idempotency-Key": activity.info().activity_id}
```

> **Why `activity.info().activity_id`?** It's *stable across retries* of the same activity execution. Every retry sees the same value, so the receiver can recognize the duplicate. If you used `uuid.uuid4()` here, each retry would generate a fresh value — defeating the whole point.

---

## 3. Verify the fix (~3 min)

Back in the [button label="Terminal" background="#444CE7"](tab-2) tab, repeat the chaos sequence — schedule the activity, then crash the worker:

```bash,run
scripts/reset-echo.sh
uv run python -m webhooks.send_standalone evt_fixed
scripts/kill-worker.sh
```

Switch to the [button label="Worker" background="#444CE7"](tab-3) tab. Press **Up Arrow + Enter** to restart the worker:

```bash,run
uv run python -m webhooks.worker
```

Wait ~20 seconds for `start_to_close_timeout` to fire on the crashed attempt and the retry to run.

Check the [button label="Echo server" background="#444CE7"](tab-4) tab. **1 delivery** for `evt_fixed` — and the record will have `"deduped": true` in its server response, signalling that the receiver saw a duplicate key and refused to re-process.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Activities** view → find `deliver-evt_fixed`. Look at the **Attempt** number — it should be 2 or higher. That's the receipt: Temporal *did* retry, but the receiver's idempotency check absorbed the duplicate.

> **What just happened?** The first POST was logged in the receiver's cache by its idempotency key. When the retry POSTed with the same key, the receiver returned the cached response without recording a new delivery. At-least-once delivery + idempotency = effectively at-most-once side effect.

---

## Check your understanding

> Your activity body generates a random discount code (`code = random.choice(codes)`) for each delivery, and you build the `Idempotency-Key` from `code`. The activity body works on the happy path. What goes wrong on a retry, and how do you fix it?

<details>
<summary>Answer</summary>

Each retry generates a **different** random code, so the idempotency key changes per attempt. The receiver sees N different keys for the same logical request and accepts all N. Your "idempotency" doesn't dedup anything.

The fix is to make the key deterministic across retries:

- Derive it from input fields the caller already chose (e.g. `req.event_id`), or
- Use `activity.info().activity_id`, which is stable across retries.

If you need a random code as part of the side effect, generate it **outside** the activity (in the caller / starter) and pass it in as activity input.

</details>

## Coming up

**Module 03** — Concurrency, rate limits, and priority. You've made each delivery safe. Next: cap how many of them fire at once so you don't DDoS your own customers.
