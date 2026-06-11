# Course Outline · Standalone Activities as a Durable Job Queue (Python)

> **Status:** outline only - full PRD deferred until this shape is approved.

## In one sentence

Build a durable webhook-delivery service with Standalone Activities - Temporal's durable job queue - and learn how the platform replaces the broker, scheduler, retry library, and result store you'd otherwise have to operate yourself.

## Audience

Python developers who already understand Temporal Activities.

## Why this course

- **Positioning:** Standalone Activities are Temporal's answer to traditional job queues - the place to land users who are operating their own background-jobs infrastructure today.
- **Public-facing tone:** generic "traditional job queues" framing; no Workflow-vs-SAA comparisons that imply Workflows are expensive; no specific competitor product names in learner-facing copy.
- **Canonical messaging source:** the Replay 2026 "Standalone Activities for durable job processing" deck and the Coinbase customer talk (https://www.youtube.com/watch?v=zsF5Y-IOMOw).
- **Eng review (Dan, Harani):** the Activity vs SAA-Job-Queue split is the right shape. Three additions baked in:
  1. Cover the operator levers (concurrency, rate limits, fairness, priority).
  2. Cover dedup at the platform (`id_conflict_policy`) separately from receiver-side idempotency.
  3. End with the "same code, two job types" upgrade path - not a SAA-vs-Workflow decision tree.

## Customer signal (internal research notes - not learner-facing copy)

- **Stripe** - webhook delivery; configurable concurrency + rate limits per customer; reliability-sensitive.
- **Rippling** - replace legacy ETA; concurrency + debounce per task.
- **Coinbase** - replace custom Background Jobs Service at 200–600M jobs/day; one-platform consolidation, configurable retention for post-incident analysis.
- **Cursor** - durable queue for agent / webhook work (Linear webhooks named).
- **Roblox** - workflow-per-item too expensive at millions–billions of items.
- **Block (Square)** - SimpleTask was "crucial for adoption"; cheap on-ramp matters.

**In one sentence:** customers want a durable, addressable, vendor-backed job queue with operator levers (concurrency, dedup, priority, fairness), and an upgrade path to multi-step orchestration without rewriting the work itself.

## Modules

Pedagogy: **do-then-understand**. Each module gets the learner doing or breaking something before any explanation, and most modules have a visible **fail-then-fix moment** that builds intuition the reading alone wouldn't. Every module body opens with the traditional-job-queue pain it addresses, then positions Standalone Activities as the platform-level fix.

| # | Name | Time | Goal | Fail-then-fix moment |
|---|------|------|------|----------------------|
| 01 | **Durable job queue** (core, build-and-teach) | 7 min | Build a webhook-delivery Activity, submit it as a Standalone Activity, inspect it in the UI. Establish what "addressable, durable, observable, no infrastructure" means. | - (introduction; the fail-then-fix arc starts in 02) |
| 02 | **Idempotency and crash safety** | 10 min | Dedup against Temporal's own retries. A Worker crash mid-flight makes Temporal retry - without an idempotency key the receiver gets the POST twice; with one, only the first lands. | `pkill` the Worker mid-flight; receiver counter climbs; add one line; counter stays at 1. |
| 03 | **Deduplication via ID reuse** | 10 min | Dedup against duplicate upstream events. Same event id submitted twice from the client → Temporal returns the existing handle at the scheduling layer (no second Worker run). | Call `start_activity` twice with same id without a conflict policy; observe error; add `USE_EXISTING`; both calls succeed with the same `run_id`. |
| 04 | **Concurrency, rate limits, priority & fairness** | 12 min | Worker config to cap downstream load (`max_activities_per_second`) **and** Task Queue Priority / Fairness to keep one loud tenant from starving others. | Unbounded fan-out → all 30 deliveries land in <1s; add `max_activities_per_second=5.0` → they spread out; add `Priority(priority_key=1)` → urgent jumps bulk. |
| 05 | **Heartbeats and checkpointing** | 10 min | Long-running Standalone Activity that processes a batch, reports progress via `activity.heartbeat(progress)`, and resumes from the last checkpoint on Worker crash via `activity.info().heartbeat_details`. | Kill the Worker mid-batch with `kill-worker.sh`; without the checkpoint read, retry redoes items; add the read; retry resumes cleanly. |
| 06 | **Same code runs anywhere** (wrap-up) | 8 min | Take the exact `deliver_webhook` Activity from Module 01 and submit it as a Standalone Activity *and* as a step inside a Workflow. Show that the same Activity code can move into a Workflow when the job becomes multi-step. Include the Coinbase citation. | - (capstone; conceptual rather than fail-then-fix) |

## Use case

Webhook delivery to a local stdlib webhook receiver inside the Instruqt sandbox. No API keys, no new Docker dependency.

## Out of scope (for v1)

- Workflows beyond the comparison wrap-up in Module 06 · signals / queries / updates · local activities · worker versioning.
- Temporal Cloud connection (deferred - was a stretch goal; the current track runs against the dev server inside the sandbox).

## Open questions for review

1. `temporalio` SDK version supports `id_conflict_policy` / `id_reuse_policy` kwargs on `start_activity` (Module 03 depends on this) - verify the pinned version in `pyproject.toml`.
2. Task Queue Priority and Fairness exposure in the Python SDK + dev server (Module 04). Verify against the pre-release CLI pinned in `python/sandbox/Dockerfile`.
3. Module 06's "same Activity, two callers" invariant: both the standalone caller and the Workflow file must import `deliver_webhook` from the same module, not duplicate the function. Enforced by the content-guardrail script (see verification plan).
