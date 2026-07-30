# PRD: Standalone Activities tutorial (Java track)

## Goal

Deliver the Java edition of "Build a Job Queue with Standalone Activities" as an Instruqt
track, at feature parity with the Python reference and the Go/TypeScript ports. Learners
build a durable webhook-delivery service and, module by module, learn to submit durable jobs,
make retries idempotent, dedup duplicate submissions, pace throughput, checkpoint long jobs
with heartbeats, and reuse the same Activity inside a Workflow.

## Audience

Java developers comfortable with Temporal Activities and Workers at the Temporal 101 level.

## Scope

- Six modules, each an independent Gradle project with `exercise/` (TODO stubs) and
  `solution/` code.
- Temporal Java SDK 1.36.1 Standalone Activities API (`ActivityClient`, `StartActivityOptions`,
  `ActivityHandle`), plus heartbeats, `WorkerOptions` rate caps, and the Workflow upgrade path.
- Shared webhook receiver reused from `shared/webhook-receiver/` (Python stdlib) rather than a
  per-language re-implementation; interactive demo HTML served on port 9001.
- Sandbox image on Temurin 21 with Gradle, Temporal CLI 1.7.2, and python3.

## Non-goals

- Temporal Cloud, auth, multi-namespace.
- Re-teaching Temporal basics (Workflows, Workers) beyond what each feature needs.

## Teaching invariants

- Open each module with the traditional-job-queue pain, then position Standalone Activities as
  the platform-level fix. Never lead with a Workflow-vs-Standalone comparison.
- Generic "traditional job queue" framing; no competitor product names in learner copy.
- Module 06 proves the same `deliverWebhook` Activity runs both standalone and as a Workflow
  step with no code change (enforced by `scripts/verify-content.sh` check 4).
