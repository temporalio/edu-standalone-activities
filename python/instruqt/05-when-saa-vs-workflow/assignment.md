---
slug: when-saa-vs-workflow
id: qsxxv9y7a5oh
type: challenge
title: When SAA vs when Workflow
teaser: Three scenarios. For each one, pick Standalone Activity or Workflow — then
  check your answer.
notes:
- type: text
  contents: |
    # When to pick a Standalone Activity vs. a Workflow

    You've now built a webhook delivery service four ways:

    - **Module 1** — events recorded for a Standalone Activity (3) vs. an Activity-in-Workflow (11).
    - **Module 2** — making retries safe with an idempotency key.
    - **Module 3** — capping throughput with a per-second rate limit, and using `Priority` to put urgent work ahead of bulk.
    - **Module 4** — rejecting duplicate `start_activity` calls at the server.

    This last module steps back and asks: when should you actually reach for a Standalone Activity in production, and when do you need a real Workflow?

    Standalone Activities aren't a replacement for Workflows — they're a lighter-weight option for the cases where you don't need orchestration. This module walks through three concrete scenarios. For each one, decide which to use before revealing the answer. Two short knowledge-check questions follow.

    No code in this module. About 8 minutes.
tabs:
- id: ripqsfaeunnx
  title: Exercise
  type: code
  hostname: workshop
  path: /root/workshop/exercises
- id: vcb9zf4htmkc
  title: Terminal
  type: terminal
  hostname: workshop
  workdir: /root/workshop/exercises
difficulty: basic
timelimit: 600
enhanced_loading: null
---

# When to pick a Standalone Activity vs. a Workflow

You've built four versions of the same service. The next decision is which approach to pick for new work.

A simple rule of thumb: **if you need orchestration — multi-step state, signals, queries, child workflows, compensation — write a Workflow. If each unit of work is self-contained, write a Standalone Activity and save the events, the retention, and the cost.**

Three scenarios below. For each one, decide your answer first, then open the disclosure to see the recommendation and reasoning.

---

## Scenario 1: Webhook delivery at 10 million per day

You're Stripe (or a Stripe-adjacent company). Every payment event triggers a webhook delivery to the customer's endpoint. Each delivery is one HTTP POST with retry on 5xx and exponential backoff. No follow-up steps, no coordination — just one durable POST per event.

**Pick an answer before you reveal.**

<details>
<summary><b>Reveal the answer</b></summary>

**Standalone Activity.**

Each delivery is a single durable side effect. No multi-step state, no signals, no compensation. A Workflow would record orchestration events you're not using — extra Workflow history per delivery, full retention, more billed actions. At 10 million per day, the cost difference is real money.

Everything Stripe needs in this scenario you've already learned:

- **Module 2**: a deterministic Idempotency-Key on the POST dedupes the retry window.
- **Module 3**: `max_activities_per_second` protects the customer's endpoint from being overloaded.
- **Module 4**: `id_conflict_policy=USE_EXISTING` absorbs duplicate events from the upstream payment system.

</details>

---

## Scenario 2: Multi-step order pipeline

A customer places an order. Your system needs to:

1. Charge the card.
2. Reserve inventory.
3. Trigger fulfillment.
4. Send a confirmation email.
5. **If any step fails after the card is charged**, refund the card and notify ops.

The pipeline has to look atomic from the customer's perspective — a partial result is unacceptable. The steps run sequentially, and a later failure has to roll earlier ones back.

**Pick an answer before you reveal.**

<details>
<summary><b>Reveal the answer</b></summary>

**Workflow.**

This is exactly what Workflows are for. You have:

- Multi-step state: which step you're on, what happened at each, what to roll back.
- Conditional logic: if the charge failed, skip the rest; if fulfillment failed, refund the charge.
- Saga semantics: refund-on-failure is a compensation step.
- Long timescales: fulfillment might take days; the Workflow waits durably for it.

You'd write each step as an Activity called from a Workflow. The Activities themselves could be Standalone Activities in a different system, but the *orchestration* belongs in a Workflow.

</details>

---

## Scenario 3: Daily user digest email

Every morning at 9 AM in the user's timezone, send them an email summarizing their account activity. The job runs once per user per day. Each user's digest is independent of every other user's. If it fails, retry with backoff. No coordination with anything else.

**Pick an answer before you reveal.**

<details>
<summary><b>Reveal the answer</b></summary>

**Standalone Activity.**

Each user's daily digest is a one-shot durable job. A scheduler (Temporal Schedules, cron, or your own logic) calls `client.execute_activity` once per user per day. The Activity:

1. Reads the user's activity for the last 24 hours.
2. Composes the email body.
3. Sends the email through your provider.

No multi-step state across Activities. No signals. The only retry you care about is at the single-Activity level, which Temporal handles natively. A Workflow here would wrap a single Activity call and pay for orchestration semantics you don't use.

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

A `Workflow` execution emits about **11 events** per Activity it runs (`WorkflowExecutionStarted` + three `WorkflowTask*` + three `ActivityTask*` + three `WorkflowTask*` + `WorkflowExecutionCompleted`). A `Standalone Activity` emits about **3 events** (`Scheduled`, `Started`, `Completed`). At 10 million activities per day, roughly how much *less* server-side bookkeeping does Standalone produce per day?

<details>
<summary>Answer</summary>

**~80 million fewer events per day** — and roughly **half the actions billed** on Temporal Cloud.

10M activities × (11 events − 3 events) = 80M fewer events recorded by Temporal each day.

The Cloud-billing math: a Workflow + Activity is at least 2 billed actions. A Standalone Activity is 1. At 10M/day that's the difference between paying for 10M actions and paying for 20M — roughly a 50% cost reduction for this class of one-shot work.

</details>

---

## Wrap-up

What you can now do with Standalone Activities in Python:

- **`client.execute_activity` / `client.start_activity`** — run an Activity directly from a client, with no Workflow class.
- **`Idempotency-Key` from a stable logical job id** — make retries safe by giving the receiver a deterministic dedup key.
- **`max_activities_per_second`** on the Worker — cap dispatch rate to protect the downstream service.
- **`ActivityIDConflictPolicy.USE_EXISTING`** — let the server absorb duplicate `start_activity` calls instead of erroring.
- **`Priority(priority_key, fairness_key, fairness_weight)`** on `start_activity` — push urgent work ahead of bulk when the queue is contended.

Click **Check** to finish the track.
