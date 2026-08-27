#!/usr/bin/env bash
# Read findings.json (from the agent) plus changes.json (from collect-changes.sh),
# defensively filter findings to changed hunks, create a GitHub Check Run with
# annotations, write a step summary, and set workflow exit code.
#
# Exit code policy:
#   - 1 if any post-filter finding has priority "High"   → required check fails
#   - 0 otherwise                                        → required check passes
#
# Required env:
#   GITHUB_REPOSITORY  — owner/repo
#   HEAD_SHA           — PR head commit (Check Run head_sha)
#   GITHUB_STEP_SUMMARY — set automatically by GitHub Actions
# Optional:
#   FINDINGS_FILE  (default: findings.json)
#   CHANGES_FILE   (default: changes.json)
#   CHECK_NAME     (default: "Canonical Temporal Review")

set -euo pipefail

: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY required}"
: "${HEAD_SHA:?HEAD_SHA required}"

FINDINGS_FILE="${FINDINGS_FILE:-findings.json}"
CHANGES_FILE="${CHANGES_FILE:-changes.json}"
CHECK_NAME="${CHECK_NAME:-Canonical Temporal Review}"
SUMMARY_FILE="${GITHUB_STEP_SUMMARY:-/dev/stderr}"

# Max annotations per Check Run create call. GitHub allows 50 per request.
MAX_ANN=50

if [[ ! -f "$FINDINGS_FILE" ]]; then
  echo "post-findings: $FINDINGS_FILE not found — agent did not produce output" >&2
  exit 2
fi
if [[ ! -f "$CHANGES_FILE" ]]; then
  echo "post-findings: $CHANGES_FILE not found" >&2
  exit 2
fi

# Defensive filter: drop findings whose start_line is not inside any hunk for the
# named path. Catches model overreach (annotating unchanged context).
filtered_json="$(jq --slurpfile changes "$CHANGES_FILE" '
  ($changes[0].files // []) as $files
  | .findings
  | map(
      . as $f
      | ($files[] | select(.path == $f.path) | .hunks // []) as $hunks
      | if any($hunks[]; .start <= $f.start_line and $f.start_line <= .end)
        then .
        else empty
        end
    )
  | { findings: . }
' "$FINDINGS_FILE")"

# Counts by priority.
high_count="$(jq '[.findings[] | select(.priority=="High")] | length' <<<"$filtered_json")"
med_count="$(jq  '[.findings[] | select(.priority=="Medium")] | length' <<<"$filtered_json")"
low_count="$(jq  '[.findings[] | select(.priority=="Low")] | length' <<<"$filtered_json")"
total_count="$(jq '.findings | length' <<<"$filtered_json")"

dropped_count="$(jq --slurpfile orig "$FINDINGS_FILE" \
  '($orig[0].findings | length) - (.findings | length)' <<<"$filtered_json")"

conclusion="success"
if [[ "$high_count" -gt 0 ]]; then
  conclusion="failure"
fi

# Build the annotations array (truncate at MAX_ANN).
annotations_json="$(jq --argjson max "$MAX_ANN" '
  .findings
  | .[0:$max]
  | map({
      path: .path,
      start_line: .start_line,
      end_line: .end_line,
      annotation_level:
        (if .priority == "High" then "failure"
         elif .priority == "Medium" then "warning"
         else "notice" end),
      title: ("[\(.priority)] Canonical Temporal Review"),
      message: ("\(.problem)\n\nFix: \(.fix)\n\nRule: \(.citation)")
    })
' <<<"$filtered_json")"

# Truncation warning, if any.
trunc_note=""
if [[ "$total_count" -gt "$MAX_ANN" ]]; then
  trunc_note=$'\n\n_Showing first '"$MAX_ANN"' of '"$total_count"' findings. Address these and re-run to surface the rest._'
fi

summary_body="$(printf '%s\n' \
  "**Canonical Temporal Review** — findings from rules pinned at \`$(cat .github/canonical-temporal-review/skill-ref.txt 2>/dev/null || echo unknown)\`." \
  "" \
  "| Priority | Count |" \
  "|---|---|" \
  "| High (blocks merge) | ${high_count} |" \
  "| Medium (advisory)   | ${med_count}  |" \
  "| Low (advisory)      | ${low_count}  |" \
  "" \
  "${dropped_count} finding(s) were dropped because they fell on unchanged lines.${trunc_note}")"

# Create the Check Run.
payload="$(jq -n \
  --arg name "$CHECK_NAME" \
  --arg head_sha "$HEAD_SHA" \
  --arg conclusion "$conclusion" \
  --arg title "Canonical Temporal Review: ${high_count} high, ${med_count} medium, ${low_count} low" \
  --arg summary "$summary_body" \
  --argjson annotations "$annotations_json" \
  '{
     name: $name,
     head_sha: $head_sha,
     status: "completed",
     conclusion: $conclusion,
     output: {
       title: $title,
       summary: $summary,
       annotations: $annotations
     }
   }')"

echo "$payload" | gh api \
  --method POST \
  -H "Accept: application/vnd.github+json" \
  "/repos/${GITHUB_REPOSITORY}/check-runs" \
  --input - > /dev/null

# Step summary for the Actions run page.
{
  echo "## Canonical Temporal Review"
  echo
  echo "**Conclusion:** \`${conclusion}\`"
  echo
  echo "| Priority | Count |"
  echo "|---|---|"
  echo "| High (blocks merge) | ${high_count} |"
  echo "| Medium (advisory)   | ${med_count}  |"
  echo "| Low (advisory)      | ${low_count}  |"
  echo
  if [[ "$dropped_count" -gt 0 ]]; then
    echo "_Dropped ${dropped_count} finding(s) on unchanged lines._"
  fi
  if [[ -n "$trunc_note" ]]; then
    echo "$trunc_note"
  fi
} >> "$SUMMARY_FILE"

if [[ "$conclusion" == "failure" ]]; then
  exit 1
fi
