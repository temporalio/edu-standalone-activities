# Agent instructions for this repo

Guidance for Claude Code (and other coding agents) working on this tutorial repo.

## PRs developed via plan mode

When a change was scoped through Claude Code's **plan mode**, the PR body must
include the plan's content - context, scope, file/module changes, and
verification steps - so reviewers can evaluate the change against the intent,
not reconstruct it from the diff.

Plan files live at `~/.claude/plans/` on the author's machine. Paste the
relevant sections into the PR body when opening (trim any open-questions
sections that were resolved during planning).

Reviewers should be able to read the PR body alone and understand:

- **Why** the change is being made (the Context section of the plan).
- **What** changed, file by file or module by module.
- **How to verify** locally (e.g., `python/scripts/verify-content.sh`,
  `instruqt track validate`, manual walkthrough checklist).

## Tutorial content rules

Codified in `claude-lessons/instruqt-tracks.md` (Instruqt-specific playbook)
and enforced by `python/scripts/verify-content.sh` (content guardrails).

Quick reference:

- **Open each module body** with the traditional-job-queue pain the module
  addresses, then position Standalone Activities as the platform-level fix.
  Never lead with a Workflow-vs-Standalone comparison.
- **Generic framing** ("traditional job queues", "DIY job queue") - do not
  name specific competitor products (Celery, Sidekiq, Faktory, etc.) in
  learner-facing copy.
- **Capitalize Temporal primitives** (Activity, Workflow, Worker, Task Queue,
  Standalone Activity, etc.) when referring to the Temporal concept. The
  phrase **"job queue" stays lowercase** - it's descriptive SEO vocabulary,
  not a Temporal primitive.
- **Each module's exercise/solution code** should carry concise inline
  comments highlighting the feature(s) that module is teaching. One short
  comment per non-obvious line, not every line.

## Before opening a PR

```bash
python/scripts/verify-content.sh                  # hard-fail content guardrails
cd python/instruqt && instruqt track validate     # Instruqt schema check
```

Both must exit zero.
