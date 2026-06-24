# Agent instructions for this repo

Guidance for Claude Code (and other coding agents) working on this tutorial repo.

## Planning mode (required tooling)

Whenever you enter **plan mode** for work in this repo, you MUST:

- Invoke the **`/temporal:temporal-developer`** skill to ground SDK,
  primitive, and runtime guidance in current Temporal best practice.
- Consult the **Temporal Docs MCP** (`https://temporal.mcp.kapa.ai`, exposed
  here as `mcp__claude_ai_Temporal_Docs__search_temporal_knowledge_sources`)
  for every factual claim about Temporal behavior, APIs, or terminology that
  ends up in the plan. Do not rely on model memory.
- Cite the doc source (URL or doc title) in the plan next to any claim that
  was verified against the Docs MCP, so reviewers can re-check it.

If a claim cannot be verified against the Docs MCP or the official Temporal
sources it surfaces, flag it explicitly as unverified in the plan rather than
asserting it as fact.

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

## Instruqt track layout

The default layout for all Instruqt tutorials in this repo:

```yaml
default_layout: AssignmentLeft
default_layout_sidebar_size: 33
```

Use these values in every `track.yml`. Do not use `AssignmentRight` or sidebar sizes other than 33 unless a specific track has a documented reason to differ.

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

## Content review (required workflow)

Any review of tutorial content - whether triggered by a `/review`, ahead of a
PR, or as part of a larger refactor - MUST:

1. **Fan out per module.** Launch one subagent per module (e.g., via the
   `Agent` tool with `subagent_type: Explore` for read-only audits or
   `general-purpose` for deeper passes). Do not review all modules inline in
   a single pass - per-module isolation keeps the context focused and the
   findings auditable.
2. **Run `/temporal:temporal-developer`** inside the review flow so SDK and
   primitive guidance comes from the maintained skill, not model memory.
3. **Verify every statement.** Each Temporal claim in the module (API
   shapes, primitive semantics, retry/timeout defaults, durability
   guarantees, naming) must be checked against the Temporal Docs MCP at
   `https://temporal.mcp.kapa.ai`. Treat unverified claims as defects and
   list them in the review output.
4. **Report per module.** Each subagent returns a structured finding list:
   verified-correct items, unverified items, and concrete fixes. The
   top-level review aggregates these without re-summarizing away the
   citations.

A review that skips the Docs MCP verification step is not a passing review,
even if no other issues are found.

## Before opening a PR

```bash
python/scripts/verify-content.sh                  # hard-fail content guardrails
cd python/instruqt && instruqt track validate     # Instruqt schema check
```

Both must exit zero.
