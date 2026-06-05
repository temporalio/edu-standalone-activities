# Creating Instruqt tracks — playbook

Concise lessons from getting `edu-standalone-activities` to "Play" on Instruqt. Append as new lessons arrive — Claude maintains this file when working on Instruqt-shaped repos.

## Quick start

```bash
# 1. Auth
brew install instruqt/tap/instruqt
instruqt auth login

# 2. Validate + push
cd <repo>/<lang>/instruqt
instruqt track validate
instruqt track push           # first push writes id + checksum back

# 3. Commit the assigned IDs
git add <lang>/instruqt/track.yml <lang>/instruqt/*/assignment.md
git commit -m "Pin Instruqt track + challenge IDs"
```

## File layout that works

```
<lang>/
├── instruqt/
│   ├── track.yml              # track-level config (no `challenges:` list!)
│   ├── config.yml             # container image reference
│   ├── track_scripts/
│   │   ├── setup-<container>  # boot services, seed /root/workshop
│   │   └── cleanup-<container>
│   └── NN-<slug>/             # one per challenge (NN = ordering prefix)
│       ├── assignment.md      # YAML frontmatter + markdown body
│       ├── setup-<container>
│       ├── solve-<container>
│       └── check-<container>  # validation gate
├── sandbox/
│   └── Dockerfile             # built + pushed to a public registry
└── course-repo/               # COPY'd to /opt/workshop in the image
    ├── pyproject.toml
    ├── exercise/<NN-slug>/    # starter (TODOs)
    └── solution/<NN-slug>/    # completed
```

Script names' suffix MUST match the container name in `config.yml`. Container `workshop` → scripts named `setup-workshop`, `check-workshop`, etc. Mismatched names → `references unknown host` error.

**Anything in a challenge folder is treated as a script.** Validator parses every file as `<event>-<container>`. Dropping `cost-comparison.svg` next to `assignment.md` fails with `references unknown event 'cost'` + `references unknown host 'comparison.svg'`. Put diagrams, assets, etc. *outside* the challenge folder (e.g. `python/diagrams/`).

**Challenge directory numbering must be sequential.** Naming directories `01-`, `02-`, `04-` errors with `challenge ids are not sequential, expected: 03 actual: 04`. Either fill in the gap or renumber. The `NN-` prefix is the ordering Instruqt enforces; renumbering on the fly is cheap because the `slug:` in the frontmatter is what's actually stable (the directory prefix can change without breaking links).

## Schema gotchas

- **Per-challenge metadata** lives in YAML frontmatter at the top of `assignment.md`. There is no `challenge.yml` file.
- **Slug** in challenge frontmatter drops the `NN-` directory prefix: directory `01-skip-the-workflow` → `slug: skip-the-workflow`.
- **`challenges:` list in track.yml is unused** — challenges are auto-discovered from subdirectories.
- **Service tabs** (in-sandbox HTTP services): `type: service` with `hostname:` + `port:`. NOT `type: website`. The `website` type requires HTTPS — `http://localhost:PORT` is rejected.
- **Service tabs open at `/`** — design in-sandbox HTTP services with a useful root endpoint, not just specific paths.
- **`lab_config.theme`** is an object: `theme: { name: modern-dark }`. Not a string.
- **`lab_config.sidebar_enabled`** is flat boolean. No `sidebar: {enabled, width}` nesting.
- **Layout fields**: `default_layout: AssignmentRight`, `default_layout_sidebar_size: 40`, `override_challenge_layout: true`.

## Image + registry

- **Instruqt does NOT build your Dockerfile.** You build and push.
- **Default registry: GHCR** (`ghcr.io/<org>/<image>`).
- **Build for `linux/amd64`.** Instruqt sandboxes are amd64. Apple Silicon's default arm64 causes "Fail to Start" *with no logs* (container never boots). Use `docker buildx build --platform linux/amd64 --push`.
- **GHCR packages start private** — toggle to Public in package settings or Instruqt can't pull. One-time UI action per package: `https://github.com/orgs/<org>/packages/container/<name>/settings` → Danger Zone → Change visibility.
- **`gh` token needs `write:packages` scope** to push images: `gh auth refresh -h github.com -s write:packages,read:packages`.
- **Build context** is the parent of `sandbox/` AND `course-repo/` (usually `<lang>/`) so the Dockerfile's `COPY course-repo/` resolves.
- **CI workflow** at `.github/workflows/build-sandbox.yml`: `docker/build-push-action@v6` with `platforms: linux/amd64`, triggered on changes under sandbox/ or course-repo/.

## First-push lifecycle

- First `instruqt track push` writes `id:` into track.yml and `id:` + per-tab IDs into challenge frontmatter. **Commit these.** Subsequent pushes match by id; without them the CLI creates a new track each time.
- If a push half-succeeds, remote has a stub. Run `instruqt track pull` → writes `*.remote` files → manually merge `id:` + `checksum:` into local, delete `.remote` files, push.
- Always `instruqt track validate` first — same schema/script checks as push but no network.

## Content authoring

- **Loading messages** (rotate during the ~20-60s sandbox provisioning): `lab_config.loadingMessages` is a list of plain strings, ~80 chars each. No markdown rendering. Aim for 15-20 entries. Mix concrete teaching with a touch of warmth.
- **Welcome / Start screen** (shown before the learner clicks Start, after they've opened the track): `notes:` field in the FIRST challenge's frontmatter. Markdown renders. Use for one-line scenario, what's already running (tab orientation), what this first chapter accomplishes.
- **In-challenge body** (the live page beside the sandbox): the markdown body of `assignment.md` (everything after the frontmatter). Full markdown.

### `code` tab `path` field — limitation

`path: <dir>` opens the editor with the file tree rooted at that directory; **no file is pre-opened**. `path: <file>` opens with the file shown but the tree only contains that single file. Instruqt has NO field for "tree at directory + file pre-opened" — only the three fields `title`, `hostname`, `path` are supported on `type: code` tabs (per docs + CLI source). Pick which trade-off you want and call out the file in the assignment text.
- **Tab-jump buttons** (clickable in-body link that focuses a tab): use the markdown-link-with-attributes form `[button label="Worker" background="#444CE7"](tab-N)` where `N` is the zero-indexed position in the `tabs:` list, NOT the tab id. Nexus reference uses `#444CE7` as the standard accent color.

### Advanced content types — known and unknown

Confirmed rendering: headings, paragraphs, bold/italic, inline code, code fences, lists, links, blockquotes, tables.

**Tested working:**
- Inline `<svg>` element in markdown body / `notes:contents:` — renders cleanly. Embed the SVG XML directly in a YAML `|` block scalar; no external file dependency, no sanitization issues observed.

- SMIL animations (`<animate>` on text, circle attributes, etc.) — render and loop cleanly. Confirmed working.

**Untested:**
- Static SVG via `![alt](diagram.svg)` — Instruqt likely doesn't serve adjacent files.
- Inline `<script>` or event handlers — almost certainly stripped.

**Confirmed working:**
- `<iframe>` from cross-origin CDN (raw.githack.com tested). Survives Instruqt's notes / assignment-body sanitizer and renders cross-origin content cleanly. **Use this for interactive HTML demos** that need JS — host the HTML page in `docs/` and iframe via raw.githack.com pointing at the branch. The Idempotency demo (Module 02) does this.

**Fallback for interactive content:** host the interactive page elsewhere (Vercel, GitHub Pages) and link out with a plain markdown link.

Test method: add a minimal example to one challenge's `notes:` or body, `instruqt track push`, view. Record results back into this file.

## Chaos demos: kill the worker correctly

When designing a "kill the worker mid-activity" demo for retry / idempotency:

- **SIGKILL (`pkill -9`), not SIGTERM.** The Temporal Python SDK + a sync activity in a `ThreadPoolExecutor` will gracefully drain in-flight work on SIGTERM — the activity finishes normally, no retry, no chaos. SIGKILL is the only way to guarantee the kind of mid-flight crash the lesson requires.
- **The starter must be non-blocking.** `client.execute_activity(...)` blocks the calling shell until the activity completes — so the learner can't run `kill-worker.sh` in the same terminal. Use `client.start_activity(...)` for the starter (also a good teaching moment: standalone activities don't tether the starter).
- **Expect to wait ~`start_to_close_timeout`** between the kill and the retry firing. Without an explicit `heartbeat_timeout`, Temporal can only detect the dead attempt by waiting for the activity-attempt timeout. Pick a `start_to_close_timeout` slightly larger than the activity body's expected duration (e.g. body sleeps 15s → timeout 20s) so the demo doesn't make learners wait minutes.
- **Standalone activities don't appear in the Workflows tab of the Temporal UI** — that tab is for Workflow Executions only. They live under the **Standalone Activities** tab. Don't claim a specific `attempt =` value in the docs — let learners read what the UI actually shows.

## Free module navigation

Default Instruqt tracks lock modules sequentially. To let learners jump to any module without completing prior ones, add `skipping_enabled: true` at the root of `track.yml`. Prerequisite: every challenge with a `check-<container>` script must also have a `solve-<container>` script (Instruqt rejects the flag otherwise — when a learner skips, Instruqt runs the solve scripts for skipped challenges so the sandbox state stays coherent). Make solve scripts cheap: e.g. `cp -rf /opt/workshop/solution/NN/. $DIR/`. The lock icons in the sidebar disappear and the "Skip to challenge?" dialog stops appearing.

## Common errors → root cause

| Symptom | Cause |
|---|---|
| "Fail to Start" + empty logs | Image architecture mismatch (arm64 vs amd64). Rebuild with `--platform linux/amd64`. |
| `Could not find the image` | GHCR package is private OR tag doesn't exist. Toggle Public; verify with `docker pull`. |
| `challenge directory and slug mismatch, expected: NN-` | Slug missing/wrong in challenge frontmatter. The `NN-` prefix must NOT be in the slug. |
| `references unknown host '<X>'` | Script suffix `-<X>` doesn't match a container name in config.yml. |
| `url: must be a valid HTTPS URL` | Tab uses `type: website` with `http://`. Switch to `type: service` + hostname + port. |
| `denied: permission_denied` on `docker push` to GHCR | gh token lacks `write:packages`. Run `gh auth refresh -s write:packages,read:packages`. |
| `There are remote changes for this track` on push | Earlier push created stub. Run `instruqt track pull`, merge, retry. |
| 401 from `curl https://ghcr.io/v2/...` on a "public" image | Normal — GHCR public reads still need an anonymous bearer token from `https://ghcr.io/token?scope=repository:<path>:pull`. Use that to verify visibility, not raw curl. |

## Recommended defaults (Temporal-flavored)

- Base image: `python:3.13-slim` (or `<lang>-slim` equivalent).
- Package manager: `uv` (`curl -LsSf https://astral.sh/uv/install.sh | sh`).
- Pre-warm `uv sync` at image-build time so the first challenge boots fast.
- Container memory: `4096` MB (matches Nexus reference; less may be tight).
- `lab_config`: `extend_ttl: 900`, `sidebar_enabled: true`, `default_layout: AssignmentRight`, `default_layout_sidebar_size: 40`, `theme: { name: modern-dark }`.
- CI: auto-build the sandbox image; do NOT auto-push the track (Temporal convention — manual `instruqt track push` keeps releases deliberate).
- Use the CLI's own template as the source of truth for unfamiliar formats: `instruqt track create` + `instruqt challenge create` generate canonical files.

## Reference repos

- Working Temporal Nexus track: `github.com/temporalio/workshop-nexus-intro-instruqt`
- This track: `github.com/temporalio/edu-standalone-activities`
- Instruqt CLI: `github.com/instruqt/cli`
