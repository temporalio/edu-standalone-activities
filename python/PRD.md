# Course Outline · Standalone Activities as a Durable Job Queue (Python)

> **Status:** outline only — full PRD deferred until this shape is approved.

## In one sentence

Build a durable webhook-delivery job with a Standalone Activity, and see how it **costs less, runs with lower overhead, and is operationally simpler** than wrapping the same work in a workflow.

## Audience

Python developers who already understand Temporal Activities.

## Why this course

- **Customer asks:** cheaper job-queue primitive with operator levers — concurrency, dedup, priority / fairness, reduced visibility / retention.
- **PR-1 review (Dan):** SAA shouldn't feel like a major new skill; teach Activities once, then SAA as the cost-saving variant; the durability demo needs idempotency.
- **Eng review (Dan, Harani):** the Activity vs SAA-Job-Queue split is the right shape. Three additions to bake in:
  1. Don't stop at cost — also compare **latency, throughput, and scheduling behavior** (Harani).
  2. Cover **Task Queue Priority and Fairness** as part of the job-queue story; flag that future Flow Control features may extend this (Dan).
  3. End with a short **"when SAA vs when workflow"** tradeoffs discussion (Harani).

## Customer signal

- **Stripe** — webhook delivery; configurable concurrency + rate limits per customer; reliability-sensitive.
- **Rippling** — replace legacy ETA; "½ the features at ¼ the cost"; concurrency + debounce per task.
- **Coinbase** — replace legacy BJS at 300M jobs/day; willing to trade visibility / retention for cost.
- **Cursor** — durable queue for agent / webhook work (Linear webhooks named).
- **Roblox** — workflow-per-item too expensive at millions–billions of items.
- **Block (Square)** — SimpleTask was "crucial for adoption"; cheap on-ramp matters.

**In one sentence:** customers want SAA as a cheaper, lower-latency, workflow-like primitive for high-volume or simple "run one thing" workloads — with concurrency, dedup, priority / fairness, and reduced visibility / retention as cost levers.

## Modules

Pedagogy: **do-then-understand**. Each module gets the learner doing or breaking something before any explanation, and every module (01 included) has a visible **fail-then-fix moment** that builds intuition the reading alone wouldn't.

| # | Name | Time | Goal | Fail-then-fix moment |
|---|------|------|------|----------------------|
| 01 | **Skip the workflow** (core, build-and-teach) | 10 min | Build a webhook-delivery activity, run it standalone, run it via a workflow, compare **events, actions, retention, latency, throughput**. | "Same activity, two scheduling shapes": learner sees the wrapped version costs more on every axis — the reveal moment. |
| 02 | Idempotency and crash safety | 10 min | **Dedup against Temporal's own retries.** A worker crash mid-flight makes Temporal retry — without an idempotency key the receiver gets the POST twice; with one, only the first lands. | `pkill` the worker mid-flight; webhook receiver counter climbs to 2; add one line of code; counter stays at 1. |
| 03 | Concurrency, rate limits, priority & fairness | 12 min | Worker config to cap downstream load (`max_concurrent_activities`, `max_activities_per_second`) **and** Task Queue Priority / Fairness to keep one loud tenant from starving others. Flag future Flow Control features. | Unbounded fan-out → downstream returns 429s; add caps + per-tenant priority queues → loud tenant no longer starves the quiet one. |
| 04 | Dedup via ID reuse | 10 min | **Dedup against duplicate upstream events.** Same event scheduled twice from the client → Temporal rejects / returns the existing handle at the scheduling layer (no second worker run). SDK behavior verified at implementation. | Call `start_activity` twice with same id without a conflict policy; observe wrong behavior; add the policy; correct behavior. |
| 05 | **Ship to Temporal Cloud** (optional advanced) | 10 min | Swap the local dev server for a Temporal Cloud namespace with mTLS auth; run the same activity against Cloud; verify in the Cloud UI. Requires the learner's own Cloud namespace + cert/key. | — (capstone, not a failure exercise) |
| 06 | **When SAA vs when workflow** (wrap-up tradeoffs) | 8 min | Apply a tradeoffs framework to three concrete scenarios; explain in plain language when SAA is the better fit and when workflow semantics (signals, child workflows, multi-step state) are worth the additional overhead. | Three scenario cards; learner predicts SAA or workflow, justifies, then sees the answer. |

## Use case

Webhook delivery to a local stdlib webhook receiver inside the Instruqt sandbox. No API keys, no new Docker dependency.

## Out of scope (for v1)

Workflows beyond a 5-line comparison class · signals / queries / updates · heartbeats (deferred to future Course 1, framed as long-running-work, not cancellation) · local activities · worker versioning. (Temporal Cloud connection is now covered, optionally, in Module 05. Task Queue Priority and Fairness covered in Module 03.)

## Open questions for review

1. Does `temporalio>=1.18` `start_activity` support an `id_conflict_policy` / `id_reuse_policy` kwarg for SAAs? Module 04 depends on this.
2. Is **Task Queue Priority and Fairness** currently exposed in the Python SDK + dev server (or pre-release / Cloud-only)? Module 03's depth depends on this. If pre-release, the module either teaches what's available today + a "coming soon" callout, or defers Priority/Fairness to a v2.
3. `httpx` (one new dep) vs. stdlib `urllib` for the HTTP client.
4. Ship all 6 modules in v1, or start with core (01) + idempotency (02) + tradeoffs wrap-up (06) and add the rest iteratively?
