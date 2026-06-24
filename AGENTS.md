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

## Instruqt track configuration

Lessons from building and shipping the TypeScript standalone-activities track.
Apply these to every track in this repo.

### Layout

```yaml
default_layout: AssignmentLeft
default_layout_sidebar_size: 33
```

Use these values in every `track.yml`. Do not use `AssignmentRight` or sidebar sizes other than 33.

### Enhanced loading

```yaml
enhanced_loading: true
```

Set this at the **top level** of `track.yml` (not under `lab_config`). Without it, the Instruqt code editor presents the challenge before the container's file-serving agent has finished registering. The first file click returns "Unauthorized" and requires a browser refresh to recover. `enhanced_loading: true` holds the loading screen until the container is fully ready.

### Maintenance flag

`maintenance: true` while iterating: only track owners and authors can launch. Flip to `maintenance: false` (or remove the field; absent = false) to ship. Never push `maintenance: false` mid-iteration; it immediately exposes the half-finished track.

### Sandbox image

Push sandbox images to the **existing public package** in this org:

```
ghcr.io/temporalio/edu-standalone-activities-sandbox:<language>-latest
```

For example, the TypeScript track uses `:typescript-latest`. Creating a new package lands it as `internal` by default; changing visibility requires `delete:packages` scope or an org admin. Using a tag on the existing public package avoids this entirely.

### Docker build for Instruqt

Instruqt runs `linux/amd64` containers. Always build with:

```bash
docker build --platform linux/amd64 -f sandbox/Dockerfile .
```

Building on Apple Silicon without `--platform` produces an `arm64` image that Instruqt cannot run.

Include `ca-certificates` in the apt install step and use `curl -fsSL` (not `wget`) for downloads inside the image. Without `ca-certificates`, HTTPS downloads fail with exit code 77 under QEMU emulation.

### Temporal CLI in the Dockerfile

Use the latest **stable** release (e.g. `v1.7.2`), not a pre-release tag. Pre-release tag names (e.g. `v1.6.2-standalone-activity`) are removed from GitHub after the feature ships to stable. The `temporal activity` subcommand is in stable CLI `v1.7.x` and later.

Asset name pattern: `temporal_cli_<version-without-v>_linux_<arch>.tar.gz`

```dockerfile
ARG TEMPORAL_VERSION=v1.7.2
RUN ARCH=$(dpkg --print-architecture) && \
    VER="${TEMPORAL_VERSION#v}" && \
    curl -fsSL "https://github.com/temporalio/cli/releases/download/${TEMPORAL_VERSION}/temporal_cli_${VER}_linux_${ARCH}.tar.gz" \
        -o /tmp/temporal.tar.gz && \
    tar -xzf /tmp/temporal.tar.gz -C /usr/local/bin temporal && \
    rm /tmp/temporal.tar.gz
```

### First push sequence

```bash
# 1. Register the slug server-side (once).
instruqt track create <slug> --title "<title>"

# 2. First push reconciles the scaffold.
instruqt track push --force

# 3. Pull the server-assigned track id and tab ids.
instruqt track pull

# 4. Commit the populated track.yml and assignment.md files.
git add instruqt/ && git commit -m "Pin Instruqt track and tab ids"
```

Never skip the pull-and-commit step. Without it, the next push creates a new track instead of updating the existing one.

## Tutorial content rules

Codified in `claude-lessons/instruqt-tracks.md` (Instruqt-specific playbook)
and enforced by `python/scripts/verify-content.sh` (content guardrails).

Quick reference:

- **Never say "kill", "killed", or "kills" for Worker or service failures.** Workers
  don't get killed in a tutorial — services go down. Use "service down", "the Worker
  goes down", "bring the service down", "the service is unavailable". This frames the
  scenario as a realistic operational event (crash, deploy, machine reboot) rather
  than a deliberate act. Applies to all text: assignment.md prose, bash comments,
  HTML demo step descriptions, badge labels, and subtitle copy.

  ✅ "Bring the service down mid-batch."
  ✅ "The service goes down mid-run."
  ✅ "Worker: down"
  ❌ "Kill the Worker mid-batch."
  ❌ "Worker: killed"

- **No em dashes anywhere.** This applies to every file in the repo: assignment.md,
  HTML demos, README files, AGENTS.md, code comments, YAML strings, JavaScript
  step descriptions. Use a period, a colon, or a hyphen instead. The em dash is
  the single most recognizable LLM writing tell and is banned universally, not
  just in learner-facing prose.

  ✅ "The POST lands. The Activity errors before reporting success."
  ✅ "The fix: one line in activities.ts"
  ❌ "The POST lands — the Activity errors before reporting success."

- **Reference JSON properties by name, never by description.** When directing
  the learner to check the Webhook receiver (or any JSON API response), cite
  the exact property and expected value. Never use vague phrases like "you
  should see one delivery recorded" or "the count should increase." Write
  `"processed_count": 1` or show a `json,nocopy` block. The learner needs to
  know exactly which field to look at and what number to expect.

  ✅ `"processed_count"` should be `1`.
  ✅ `json,nocopy` block showing `{"received_count": 3, "processed_count": 1}`
  ❌ "You should see one delivery recorded."
  ❌ "The receiver shows 1 processed delivery."

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
