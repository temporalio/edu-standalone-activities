---
slug: dedup-via-id-reuse
id: 8828ruounasf
type: challenge
title: Dedup via ID reuse
teaser: Upstream sends the same event twice. Make Temporal reject the duplicate before
  any worker sees it.
notes:
- type: text
  contents: |
    # Dedup via ID reuse

    Module 02 made each activity safe under **Temporal's own retries**.
    This module handles a different duplicate problem: the upstream
    system (Stripe, GitHub, your own customer's service) sends the
    same logical event twice and calls `start_activity` twice.

    Without a policy, the second call to `start_activity` with the same
    `id` errors by default (`ActivityIDConflictPolicy.FAIL`). The fix
    is to set the policy to `USE_EXISTING` — the second call quietly
    returns the existing handle, no new activity is scheduled, no
    error to handle in your application code.

    Two layers of dedup, working together:

    - Module 02 (idempotency in the activity body): protects against
      Temporal's *own* retries after the worker crashes.
    - This module (scheduling-layer id policy): protects against
      duplicate `start_activity` calls from your application.
tabs:
- id: auimaunpq1xo
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises/04-dedup-via-id-reuse/exercise
- id: 0jbcdh3rosbw
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/exercises/04-dedup-via-id-reuse/solution
- id: luyh7wot41gn
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/04-dedup-via-id-reuse/exercise
- id: osmbuuft6ify
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises/04-dedup-via-id-reuse/exercise
- id: 3ixuq3zgdzyi
  title: Echo server
  type: service
  hostname: workshop
  port: 9000
- id: yjiy5fvrf4kf
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
difficulty: basic
timelimit: 900
enhanced_loading: null
---

# Dedup via ID reuse

By the end you'll have:

- Reproduced the default behavior: two `start_activity` calls with the same `id` and the second one errors.
- Added `id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING` so the duplicate quietly maps to the first call's handle.
- Understood how this scheduling-layer dedup composes with Module 02's body-level idempotency.

Budget ~10 minutes.

---

## 1. See the conflict (~3 min)

In the [button label="Worker" background="#444CE7"](tab-3) tab:

```bash,run
uv run python -m webhooks.worker
```

In the [button label="Terminal" background="#444CE7"](tab-2) tab:

```bash,run
scripts/reset-echo.sh
uv run python -m webhooks.send_double evt_dup_001
```

You'll see something like:

```
[call-1] start_activity id=deliver-evt_dup_001
[call-1] handle ok (run_id=...)
[call-2] start_activity id=deliver-evt_dup_001
[call-2] FAILED: WorkflowAlreadyStartedError: ...
[call-1] activity completed
```

The first call succeeded; the second call errored because Temporal's default `id_conflict_policy` is `FAIL` — refuse to schedule a second activity with an id already in flight.

> **What just happened?** Temporal's server is enforcing id uniqueness at *scheduling* time. The second call never reached a worker; it was rejected by the server. Echo server has 1 delivery, which is the right outcome — but your application code had to deal with an exception. If your upstream sends every event 1.1× on average due to retries, you'd be try/except'ing a lot.

---

## 2. Set the conflict policy (~2 min)

Open `src/webhooks/send_double.py` in the [button label="Exercise" background="#444CE7"](tab-0) tab. Two TODOs:

```python
# At the top of the file, uncomment:
from temporalio.common import ActivityIDConflictPolicy

# Inside start_activity(...), add:
id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING,
```

`USE_EXISTING` says: if there's already an activity with this id in flight, give me back a handle to *that* one instead of erroring.

---

## 3. Verify (~3 min)

Back in the [button label="Terminal" background="#444CE7"](tab-2) tab, re-run with the policy in place:

```bash,run
scripts/reset-echo.sh
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

Both calls returned successfully with the **same `run_id`** — call-2 got the existing handle. The [button label="Echo server" background="#444CE7"](tab-4) tab shows **1** delivery for `evt_dup_002`.

Open the [button label="Temporal UI" background="#444CE7"](tab-5) tab — there is exactly **one** activity for `deliver-evt_dup_002`, not two. The duplicate was rejected at scheduling time; no worker cycles were wasted.

> **What just happened?** The server-side scheduling layer absorbed the duplicate. Your application code stayed clean. Combined with Module 02's idempotency, your delivery is at-most-once-effect even when both the upstream system retries *and* Temporal retries.

---

## Going further: the reuse policy

`id_conflict_policy` handles duplicates while the original is **in flight**. Its sibling `id_reuse_policy` handles duplicates **after the original completed** — values include `REJECT_DUPLICATE` (refuse to re-run a completed id), `ALLOW_DUPLICATE_FAILED_ONLY` (re-run only if previous failed), and the default `ALLOW_DUPLICATE`. The two policies pair: conflict for "right now," reuse for "ever ran before."

---

## Check your understanding

> You set `id_conflict_policy=USE_EXISTING` and call `start_activity(id="evt_001")` twice. The second call arrives **60 seconds after** the first one already completed successfully. What happens?

<details>
<summary>Answer</summary>

A **new** activity execution starts.

`id_conflict_policy` only governs duplicates while the original is **in flight**. Once the first activity completes, `id_reuse_policy` takes over — and the default (`ALLOW_DUPLICATE`) accepts a fresh execution with the same id.

For full dedup across both windows, set both:

```python
id_conflict_policy=ActivityIDConflictPolicy.USE_EXISTING,   # in-flight duplicate
id_reuse_policy=ActivityIDReusePolicy.REJECT_DUPLICATE,     # completed duplicate
```

</details>

## Check

Press **Check** when running `send_double` with the policy in place produces 1 echo delivery and both calls return the same `run_id`.

---

## Coming up

**Module 05** — When to choose Standalone Activity vs. Workflow. Three scenarios, your call.
