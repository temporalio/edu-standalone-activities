# Course Outline · Standalone Activities as a Durable Job Queue (Python)

> **Status:** outline only — full PRD deferred until this shape is approved.

## In one sentence

Build a durable webhook-delivery job with a Standalone Activity, and see how it costs less than wrapping the same work in a workflow.

## Audience

Python developers who already understand Temporal Activities.

## Why this course

- **Customer asks:** cheaper job-queue primitive with operator levers — concurrency control, dedup, reduced visibility / retention.
- **PR-1 review (Dan):** SAA shouldn't feel like a major new skill; teach Activities once (future Course 1), then SAA as the cost-saving variant; the durability demo needs idempotency.

## Customer signal

- **Stripe** — webhook delivery; configurable concurrency + rate limits per customer; reliability-sensitive.
- **Rippling** — replace legacy ETA; "½ the features at ¼ the cost"; concurrency + debounce per task.
- **Coinbase** — replace legacy BJS at 300M jobs/day; willing to trade visibility / retention for cost.
- **Cursor** — durable queue for agent / webhook work (Linear webhooks named).
- **Roblox** — workflow-per-item too expensive at millions–billions of items.
- **Block (Square)** — SimpleTask was "crucial for adoption"; cheap on-ramp matters.

**In one sentence:** customers want SAA as a cheaper, workflow-like primitive for high-volume or simple "run one thing" workloads — with concurrency, dedup, and reduced visibility / retention as cost levers.

## Modules

| # | Name | Time | Goal |
|---|------|------|------|
| 01 | **Skip the workflow** (core, build-and-teach) | 10 min | Build a webhook-delivery activity, run it standalone, run it via a workflow, see the cost diff (events / actions / retention). |
| 02 | Idempotency and crash safety | 10 min | Without an idempotency key, crashing mid-flight double-delivers; with one, it does not. |
| 03 | Concurrency and rate limits | 10 min | Worker config to cap downstream load (`max_concurrent_activities`, `max_activities_per_second`). |
| 04 | Dedup via ID reuse | 10 min | Activity ID as scheduling-layer dedup (SDK behavior verified at implementation). |

## Use case

Webhook delivery to a local stdlib echo server inside the Instruqt sandbox. No API keys, no new Docker dependency.

## Out of scope (for v1)

Workflows beyond a 5-line comparison class · signals / queries / updates · heartbeats (deferred to future Course 1, framed as long-running-work, not cancellation) · local activities · worker versioning · Temporal Cloud connection setup.

## Open questions for review

1. Does `temporalio>=1.18` `start_activity` support an `id_conflict_policy` / `id_reuse_policy` kwarg for SAAs? Module 04 depends on this.
2. `httpx` (one new dep) vs. stdlib `urllib` for the HTTP client.
3. Ship all 4 modules in v1, or just core + one advanced module to validate the shape first?
