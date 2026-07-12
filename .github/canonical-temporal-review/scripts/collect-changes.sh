#!/usr/bin/env bash
# Emit changes.json describing the in-scope files changed in this PR and the
# inclusive line ranges of their hunks at HEAD. Consumed by the canonical
# Temporal review agent (see prompt.md) and by post-findings.sh (for the
# defensive hunk filter).
#
# Required env:
#   BASE_SHA  — merge base or PR base commit
#   HEAD_SHA  — PR head commit
# Optional:
#   $1        — output path (default: changes.json in CWD)

set -euo pipefail

: "${BASE_SHA:?BASE_SHA required}"
: "${HEAD_SHA:?HEAD_SHA required}"

OUT="${1:-changes.json}"

# In-scope = under python/, suffix .py or .md.
# Out-of-scope at file level (paths-ignore at trigger handles most, but we
# also defend here for safety): lock files, generated, vendored, dotfiles.
in_scope() {
  local p="$1"
  [[ "$p" =~ ^python/.+\.(py|md)$ ]] || return 1
  case "$p" in
    *.lock|*.generated.*|vendor/*|*/vendor/*|*/.venv/*|.venv/*) return 1 ;;
  esac
  return 0
}

# Print "start end" (inclusive) for each hunk in $path between BASE_SHA..HEAD_SHA.
# Reads `@@ -a,b +c,d @@` headers; d defaults to 1 when omitted.
# Uses sed (POSIX) and awk (POSIX) so it runs the same on BSD and GNU.
hunks_for() {
  local path="$1"
  git diff --unified=0 "$BASE_SHA" "$HEAD_SHA" -- "$path" \
    | sed -nE 's/^@@ -[0-9]+(,[0-9]+)? \+([0-9]+)(,([0-9]+))? @@.*/\2 \4/p' \
    | awk '{
        start = $1 + 0
        count = ($2 == "") ? 1 : $2 + 0
        if (count == 0) next                # pure deletion; nothing to annotate
        printf("%d %d\n", start, start + count - 1)
      }'
}

tmp_files_json="$(mktemp)"
trap 'rm -f "$tmp_files_json"' EXIT

echo "[]" > "$tmp_files_json"

while IFS= read -r path; do
  [[ -z "$path" ]] && continue
  in_scope "$path" || continue
  # Skip files that no longer exist at HEAD (deletions). diff-filter=d below
  # already drops those, but belt and suspenders.
  [[ -f "$path" ]] || continue

  hunks_json="$(hunks_for "$path" \
    | jq -Rn '
        [inputs | split(" ") | {start: (.[0]|tonumber), end: (.[1]|tonumber)}]
      ')"

  # Skip files whose only diff is metadata (no + hunks).
  hunk_count="$(jq 'length' <<<"$hunks_json")"
  [[ "$hunk_count" -gt 0 ]] || continue

  jq --arg path "$path" --argjson hunks "$hunks_json" \
    '. + [{path: $path, hunks: $hunks}]' \
    "$tmp_files_json" > "${tmp_files_json}.new"
  mv "${tmp_files_json}.new" "$tmp_files_json"
done < <(git diff --name-only --diff-filter=d "$BASE_SHA" "$HEAD_SHA")

jq --arg base "$BASE_SHA" --arg head "$HEAD_SHA" \
  '{base_sha: $base, head_sha: $head, files: .}' \
  "$tmp_files_json" > "$OUT"

file_count="$(jq '.files | length' "$OUT")"
echo "collect-changes: wrote $OUT (${file_count} in-scope file(s))" >&2
