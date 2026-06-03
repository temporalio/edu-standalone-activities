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

### Advanced content types — known and unknown

Confirmed rendering: headings, paragraphs, bold/italic, inline code, code fences, lists, links, blockquotes, tables.

**Untested as of writing — needs a quick experiment if you want to use:**
- Static SVG via `![alt](diagram.svg)` — should work like any image.
- Inline `<svg>` element in the markdown body — depends on sanitizer; probably allowed.
- SVG with CSS/SMIL animations (no JS) — probably renders.
- `<iframe>` for embedded pages — most markdown renderers strip these. Unlikely.
- Inline `<script>` or event handlers — almost certainly stripped.

**Fallback for interactive content:** host the interactive page elsewhere (Vercel, GitHub Pages) and link out with a plain markdown link.

Test method: add a minimal example to one challenge's `notes:` or body, `instruqt track push`, view. Record results back into this file.

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
