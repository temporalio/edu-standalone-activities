---
slug: dedup-via-id-reuse
id: bm0pif1ryecr
type: challenge
title: Dedup via ID reuse
teaser: Upstream sends the same event twice. Make Temporal reject the duplicate before
  any Worker sees it.
notes:
- type: text
  contents: |
    # Dedup via ID reuse

    Module 02 made each Activity safe under **Temporal's own retries**.
    This module handles a different duplicate problem: the upstream
    system (Stripe, GitHub, your own customer's service) sends the
    same logical event twice and calls `start_activity` twice.

    Without a policy, the second call to `start_activity` with the same
    `id` errors by default (`ActivityIDConflictPolicy.FAIL`). The fix
    is to set the policy to `USE_EXISTING` — the second call quietly
    returns the existing handle, no new Activity is scheduled, no
    error to handle in your application code.

    Two layers of dedup, working together:

    - Module 02 (idempotency in the Activity body): protects against
      Temporal's *own* retries after the Worker crashes.
    - This module (scheduling-layer id policy): protects against
      duplicate `start_activity` calls from your application.
tabs:
- id: bb5mshhnqrcy
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/03-dedup-via-id-reuse/exercise
- id: xfjezxxvtuiy
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/03-dedup-via-id-reuse/solution
- id: 0dwpyxjjjkbf
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/03-dedup-via-id-reuse/exercise
- id: y8joxegs9dvn
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/03-dedup-via-id-reuse/exercise
- id: qjn5ptmaety6
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
- id: d6koeig93gmy
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Reject duplicate jobs at the platform

Traditional job queues either dedupe in the wrong place (the consumer, after a Worker has already picked up the job) or not at all — so you end up writing per-service dedup logic that behaves a little differently every time.

Standalone Activities dedupe at the Temporal server, before any Worker sees the job. When your upstream calls `start_activity` twice with the same id, you control what happens: error out (the default), or quietly return the existing handle (`USE_EXISTING`). Zero Worker cycles spent on the duplicate.

You'll do three things in this module:

1. Run two `start_activity` calls with the same id, back-to-back. Watch the second one error out.
2. Add `id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING` to the call. Re-run. Watch both calls succeed with the same `run_id`.
3. See how this scheduling-layer dedup composes with the body-level idempotency from Module 02.

The **Solution** tab has the finished code. Estimated time: 10 minutes.

---

## 1. See the conflict (~3 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab, start the Worker:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab, run `send_double` — a script that calls `start_activity` twice with the same id, back-to-back:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_double evt_dup_001
```

You should see:

```
[call-1] start_activity id=deliver-evt_dup_001
[call-1] handle ok (run_id=...)
[call-2] start_activity id=deliver-evt_dup_001
[call-2] FAILED: ActivityAlreadyStartedError: ...
[call-1] activity completed
```

The first call succeeded. The second call raised an error because the default `id_conflict_policy` is `FAIL` — the server refuses to schedule a second Activity with an id that's already in flight.

### Confirming this is the right outcome

The Temporal UI doesn't show much here, because **the server rejected the second call before any second Activity got created**. There's no failed Activity record to look at — the only sign of the rejection is the `ActivityAlreadyStartedError` your Python code caught. To confirm what happened, look at the surrounding state. In the [button label="Terminal" background="#444CE7"](tab-2) tab:

```bash,run
# Exactly 1 webhook was actually delivered (the duplicate never reached a Worker).
curl -s http://localhost:9000/_received | jq '.count, [.deliveries[].body.event_id]'

# Exactly 1 Activity exists on the server for this id.
temporal activity list --address localhost:7233 \
  --query 'ActivityId="deliver-evt_dup_001"' -o json | jq 'length'

# That one Activity ran exactly once (attempt=1, status=Completed).
temporal activity describe --address localhost:7233 \
  --activity-id deliver-evt_dup_001 -o json | jq '{attempt, status}'
```

All three checks agree: one Activity scheduled, one webhook delivered, one attempt. The rejected call cost zero Worker cycles and left no record on the server — but your application code still had to handle the exception. If your upstream sends every event 1.1× on average due to retries, you'd be try/except'ing every single call.

---

## 2. Set the conflict policy to USE_EXISTING (~2 min)

Open `src/webhooks/send_double.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. There are two `TODO` comments — uncomment the import, and add one keyword argument inside `start_activity(...)`:

```python
# At the top of the file, uncomment:
from temporalio.common import ActivityIDConflictPolicy

# Inside start_activity(...), add:
id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING,
```

The finished file is in the **Solution** tab.

`USE_EXISTING` tells the server: if there's already an Activity with this id in flight, return a handle to *that* one instead of raising an error.

---

## 3. Verify the fix (~3 min)

Back in the [button label="Terminal" background="#444CE7"](tab-2) tab, re-run with the policy in place:

```bash,run
scripts/reset-receiver.sh
uv run python -m webhooks.send_double evt_dup_002
```

Now you should see:

```
[call-1] start_activity id=deliver-evt_dup_002
[call-1] handle ok (run_id=<X>)
[call-2] start_activity id=deliver-evt_dup_002
[call-2] handle ok (run_id=<X>)         # same run_id as call-1
[call-1] activity completed
[call-2] activity completed
```

Both calls returned successfully with the **same `run_id`**. The second call got a handle to the Activity the first call scheduled, so `await handle.result()` on either handle resolves to the same outcome.

The [button label="Webhook receiver" background="#444CE7"](tab-4) tab shows **1** delivery for `evt_dup_002`. The [button label="Temporal UI" background="#444CE7"](tab-5) tab → **Standalone Activities** shows exactly one Activity record for `deliver-evt_dup_002`, not two.

> **The takeaway:** the server absorbed the duplicate call before any Worker saw it. Combined with the receiver-side idempotency from Module 02, your delivery is protected from both Temporal's own retries *and* your upstream system's duplicate calls — two different sources of duplication, two different layers of defense.

---

## Going further: the reuse policy

`id_conflict_policy` handles duplicates while the original is **in flight**. Its sibling `id_reuse_policy` handles duplicates **after the original completed** — values include `REJECT_DUPLICATE` (refuse to re-run a completed id), `ALLOW_DUPLICATE_FAILED_ONLY` (re-run only if previous failed), and the default `ALLOW_DUPLICATE`. The two policies pair: conflict for "right now," reuse for "ever ran before."

---

## Check your understanding

> You set `id_conflict_policy=USE_EXISTING` and call `start_activity(id="evt_001")` twice. The second call arrives **60 seconds after** the first one already completed successfully. What happens?

<details>
<summary>Answer</summary>

A **new** Activity execution starts.

`id_conflict_policy` only governs duplicates while the original is **in flight**. Once the first Activity completes, `id_reuse_policy` takes over — and the default (`ALLOW_DUPLICATE`) accepts a fresh execution with the same id.

For full dedup across both windows, set both:

```python
id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING,   # in-flight duplicate
id_reuse_policy=ActivityIDReusePolicy.REJECT_DUPLICATE,     # completed duplicate
```

</details>

## Coming up

**Module 04** — Concurrency, rate limits, and priority. Your jobs dedupe correctly now. Next: cap how many run at the same time so a burst of submissions doesn't DDoS the receiver — and prioritize urgent ones over bulk.
