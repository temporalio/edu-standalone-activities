---
slug: when-saa-vs-workflow
id: upzjbaybeiaf
type: challenge
title: When SAA vs when Workflow
teaser: Three scenarios. For each one, pick Standalone Activity or Workflow — then
  check your answer.
notes:
- type: text
  contents: |
    # When SAA vs when Workflow

    You've built a webhook delivery service four ways now. You've
    seen the cost shape (Module 01), the durability gap idempotency
    closes (Module 02), the throughput knobs (Module 03), and the
    scheduling-layer dedup (Module 04).

    Time to step back. Standalone Activities are not a replacement
    for Workflows — they're a *lighter-weight option* when you don't
    need orchestration.

    This module walks through three real-world scenarios. For each
    one, decide: SAA or Workflow? Then reveal the answer and the
    reasoning. Two knowledge-check questions at the end.

    No code. ~8 minutes.
tabs:
- id: u5kezyyfpdam
  title: Editor
  type: code
  hostname: workshop
  path: /root/workshop/exercises
- id: xyytuusarc0c
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises
difficulty: basic
timelimit: 600
enhanced_loading: null
---

# When SAA vs when Workflow

You've built it four ways. Now build a mental model for which to reach for next time.

The rule of thumb: **if you need orchestration — multi-step state, signals, queries, child workflows, compensation — write a Workflow. Otherwise, write a Standalone Activity and save the events, the retention, and the cost.**

Three scenarios. Decide your answer for each, then expand the disclosure to see the recommendation.

---

## Scenario 1: Webhook delivery at 10M/day

You're Stripe (or a Stripe-adjacent product). Every payment event triggers a webhook delivery to the customer's endpoint. Each delivery is one HTTP POST with retry on 5xx and exponential backoff. No follow-up, no coordination — fire and forget, durably.

**Decide before you peek.**

<details>
<summary><b>Recommendation: Standalone Activity.</b></summary>

Each delivery is a single durable side effect. No multi-step state, no signals, no compensation. A Workflow would pay for orchestration you're not using — extra workflow history events per delivery, full retention, more billed actions. At 10M/day the cost delta is real money.

The SAA toolkit you've already learned covers everything Stripe needs here:

- Module 02: Idempotency-Key dedups the retry-after-crash window.
- Module 03: `max_activities_per_second` keeps you from DDoSing the customer's endpoint.
- Module 04: `id_conflict_policy=USE_EXISTING` absorbs upstream duplicate events.

</details>

---

## Scenario 2: Multi-step order pipeline

A customer places an order. Your system needs to:

1. Charge the card.
2. Reserve inventory.
3. Trigger fulfillment.
4. Send a confirmation email.
5. **If any step fails after charging**, refund the card and notify ops.

The whole thing has to look atomic from the customer's perspective — partial completion is unacceptable. The shape is sequential, with conditional compensation.

**Decide.**

<details>
<summary><b>Recommendation: Workflow.</b></summary>

This is exactly what Workflows exist for. You have:

- Multi-step state (which step are we on, what happened at each).
- Conditional logic (charge failed? skip the rest. fulfillment failed? compensate the charge).
- Saga semantics (refund-on-failure is compensation).
- Long timescales (fulfillment might take days; the workflow waits durably).

You'd write each step as an Activity called from a Workflow. The Activities themselves could be Standalone in a different system, but the *orchestration* belongs in a Workflow.

</details>

---

## Scenario 3: Daily user digest email

Every morning at 9 AM in the user's timezone, send them a digest email summarizing their account activity. The job runs once per user per day. Each user's digest is independent. If it fails, retry with backoff. No coordination with anything else.

**Decide.**

<details>
<summary><b>Recommendation: Standalone Activity.</b></summary>

Each user's daily digest is a one-shot durable job. A scheduler (Temporal Schedules, cron, or your own logic) fires `client.execute_activity` per user per day. The activity:

1. Reads the user's activity for the last 24h.
2. Composes the email body.
3. Sends via your email provider.

No orchestration. No multi-step state across activities. The only retry you care about is at the single-activity level, which Temporal handles natively.

A Workflow here would just wrap a single activity call — paying the cost of orchestration semantics you don't use.

</details>

---

## Knowledge check

### Q1

Your worker is mid-execution of an activity when the host kernel panics. Where does the in-flight activity state live at that moment?

<details>
<summary>Answer</summary>

**In the worker's RAM** — and it dies with the worker.

The activity's *schedule* lives durably on the Temporal server, so when a new worker picks up the work, Temporal can dispatch the same activity again. But the in-progress work inside the activity body (variables, half-completed loops, HTTP requests mid-flight) is gone.

This is why **idempotency** in the activity body (Module 02) matters: Temporal retries from the start, not from "where you were." If the external side effect already happened, you need to dedup it or you'll do it twice.

</details>

### Q2

A `Workflow` execution emits about 10 events per Activity it runs (workflow start, workflow task scheduled/started/completed bracketing the activity events, workflow completion). A `Standalone Activity` emits about 3 events (Scheduled, Started, Completed). At 10 million activities per day, roughly how much *less* server-side bookkeeping does Standalone produce per day?

<details>
<summary>Answer</summary>

**~70 million events per day saved.**

10M activities × (10 events − 3 events) = 70M fewer events.

Multiply by the per-event cost on Temporal Cloud (and the retention storage cost), and you have a meaningful line item. This is the empirical answer to "why do Stripe / Coinbase / Roblox keep asking for Standalone Activities as a Job Queue replacement."

</details>

---

## Wrap-up

You've built and shipped a real Standalone-Activity-as-Job-Queue track. Here's what's in your toolkit now:

- **`client.execute_activity` / `client.start_activity`** — invoke an activity directly, no Workflow class needed.
- **`Idempotency-Key` from `activity.info().activity_id`** — make retries safe.
- **`max_activities_per_second`** on the Worker — cap downstream load.
- **`ActivityIDConflictPolicy.USE_EXISTING`** — dedup duplicate upstream calls.
- **`Priority(priority_key, fairness_key, fairness_weight)`** on `start_activity` — express tenant prioritization (covered briefly in Module 03, deeper in a future module).

Press **Check** to finish.
