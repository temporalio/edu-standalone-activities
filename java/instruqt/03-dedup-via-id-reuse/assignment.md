---
slug: dedup-via-id-reuse
id: hbwpfcwnujew
type: challenge
title: Dedup via ID reuse
teaser: Upstream sends the same event twice. Make Temporal reject the duplicate before
  any Worker sees it.
notes:
- type: text
  contents: |
    # Dedup via ID reuse (Java)

    Module 02 made each Activity safe under Temporal's own retries.
    This module handles a different duplicate problem: the upstream
    system (Stripe, GitHub, your own customer's service) sends the
    same logical event twice and calls ActivityClient.start twice.

    Without a policy, the second call with the same Activity ID errors by
    default (conflict policy FAIL). The fix is to set the policy to
    USE_EXISTING. The second call quietly returns the existing handle,
    no new Activity is scheduled, no error to handle.

    Two layers of dedup, working together:

    - Module 02 (idempotency in the Activity body): protects against
      Temporal's own retries after the Worker crashes.
    - This module (scheduling-layer id policy): protects against
      duplicate start calls from your application.
tabs:
- id: qybtssvgxp6j
  title: Temporal UI
  type: service
  hostname: workshop
  port: 8233
- id: sxg7yxi8rdf1
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercise/03-dedup-via-id-reuse
- id: ur8svehywhbf
  title: Solution
  type: code
  hostname: workshop
  path: /root/workshop/solution/03-dedup-via-id-reuse
- id: uoqftom2ngyj
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercise/03-dedup-via-id-reuse
- id: fpc1flzb9b37
  title: Worker
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercise/03-dedup-via-id-reuse
- id: sx3xpa4liazv
  title: Webhook receiver
  type: service
  hostname: workshop
  port: 9000
difficulty: basic
timelimit: 1500
enhanced_loading: null
---

# Reject duplicate jobs at the platform

Many job queues dedupe in the wrong place, after a Worker has already picked up the job, or they don't dedupe at all. You end up writing per-service dedup logic that behaves a little differently each time.

Standalone Activities dedupe at the Temporal server, before any Worker sees the job. When your upstream calls `ActivityClient.start` twice with the same Activity ID, you control what happens: error out (the default), or quietly return the existing handle (`USE_EXISTING`). Zero Worker cycles spent on the duplicate.

You'll do three things in this module:

1. Run two `ActivityClient.start` calls with the same Activity ID, back-to-back. Watch the second one error out.
2. Add `.setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)` to the call. Re-run. Watch both calls succeed with the same Activity ID.
3. See how this scheduling-layer dedup composes with the receiver-side idempotency key from Module 02.

The **Solution** tab has the finished code. Estimated time: 10 minutes.

---

## 1. See the conflict (~3 min)

In the [button label="Worker" background="#444CE7"](tab-4) tab, start the Worker:

```bash,run
# Start the Worker
gradle -q execute -PmainClass=webhook.Worker
```

In the [button label="Terminal" background="#444CE7"](tab-3) tab, run `SendDouble` (`SendDouble.java`), a program that calls `ActivityClient.start` twice with the same Activity ID:

```bash,run
# Reset the receiver, then call start twice with the same id. Second call errors.
scripts/reset-receiver.sh
gradle -q execute -PmainClass=webhook.SendDouble -PappArgs=evt_dup_001
```

You should see:

```bash,nocopy
[call-1] start activityId=deliver-evt_dup_001
[call-1] handle ok (activityId=deliver-evt_dup_001)
[call-2] start activityId=deliver-evt_dup_001
[call-2] FAILED: ...already started...
[call-1] activity completed
```

The first call succeeded. The second call threw because the default conflict policy is `FAIL`. The server refuses to schedule a second Activity with an id that's already in flight.

The [button label="Webhook receiver" background="#444CE7"](tab-5) tab shows `"processed_count": 1`. The duplicate never reached a Worker, but your application code still had to handle the exception. If your upstream sends some events more than once, that error handling becomes part of your normal code path.

---

## 2. Set the conflict policy to USE_EXISTING (~2 min)

Open `SendDouble.java` in the [button label="Exercise" background="#444CE7"](tab-1) tab. Add one line to the `StartActivityOptions` builder in the `start` method:

```java
.setIdConflictPolicy(
    io.temporal.api.enums.v1.ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)
```

The finished file is in the **Solution** tab.

`USE_EXISTING` tells the server: if there's already an Activity with this id in flight, return a handle to *that one* instead of throwing.

---

## 3. Verify the fix (~3 min)

Back in the [button label="Terminal" background="#444CE7"](tab-3) tab, re-run with the policy in place:

```bash,run
# Reset and retry with USE_EXISTING. Both calls should return a handle.
scripts/reset-receiver.sh
gradle -q execute -PmainClass=webhook.SendDouble -PappArgs=evt_dup_002
```

Now you should see:

```bash,nocopy
[call-1] start activityId=deliver-evt_dup_002
[call-1] handle ok (activityId=deliver-evt_dup_002)
[call-2] start activityId=deliver-evt_dup_002
[call-2] handle ok (activityId=deliver-evt_dup_002)
[call-1] activity completed
[call-2] activity completed
```

Both calls returned successfully. The second call got a handle to the Activity the first call scheduled, so `getResult()` on either handle resolves to the same outcome.

The [button label="Webhook receiver" background="#444CE7"](tab-5) tab shows `"processed_count": 1` for `evt_dup_002`. The [button label="Temporal UI" background="#444CE7"](tab-0) tab, **Standalone Activities**, shows exactly one Activity record for `deliver-evt_dup_002`, not two.

> **The takeaway:** the server handled the duplicate call before any Worker saw it. Combined with the receiver-side idempotency from Module 02, your delivery is protected from Temporal retries and from duplicate calls in your own application.

---

## Check your understanding

> You set the conflict policy to `USE_EXISTING` and call `ActivityClient.start` with the same Activity ID twice. The second call arrives **60 seconds after** the first one already completed. What happens?

<details>
<summary>Reveal the answer</summary>

A **new** Activity execution starts.

The conflict policy only governs duplicates while the original is **in flight**. Once the first Activity completes, the *reuse* policy takes over. The default (`ALLOW_DUPLICATE`) accepts a fresh execution with the same Activity ID.

For full dedup across both windows, set both:

```java
.setIdConflictPolicy(ActivityIdConflictPolicy.ACTIVITY_ID_CONFLICT_POLICY_USE_EXISTING)   // in-flight duplicate
.setIdReusePolicy(ActivityIdReusePolicy.ACTIVITY_ID_REUSE_POLICY_REJECT_DUPLICATE)        // completed duplicate
```

`REJECT_DUPLICATE` does not silently return the existing result the way `USE_EXISTING` does. A post-completion duplicate throws, so you still handle that in the after-completion window.

</details>

## Coming up

**Module 04**: Concurrency, rate limits, and priority. Your jobs dedupe correctly now. Next, cap how many run at the same time so a burst of submissions does not overwhelm the receiver.

---

📝 **Feedback on this tutorial?** [Share your thoughts in our quick form](https://forms.gle/hbTUjkHB6dkucEg27). It helps us improve.
