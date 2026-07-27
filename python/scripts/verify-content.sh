#!/usr/bin/env bash
# Content guardrails for the SAA tutorial.
#
# Hard-fails the build if any of these slip back in:
#   - Banned messaging phrases (SAA-vs-Workflow framing, event-count math,
#     cost-comparison vocabulary)
#   - Specific competitor product names in learner-facing copy
#   - Slug / directory name mismatches in Instruqt assignment frontmatter
#   - Module 06's "same Activity, two callers" invariant (both callers must
#     import deliver_webhook from the same .activities module)
#   - Standalone Activities UI over-claims (the tab shows one record + an
#     attempt counter, NOT a per-attempt "retry history", per-attempt
#     drill-in, or a specific "attempt = N" value)
#
# Run locally before `instruqt track push` or git push.
# Exit 0 = clean, non-zero = problems found.

set -uo pipefail  # no -e: we want to run every check and report all failures.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOP_ROOT="$(cd "$REPO_ROOT/.." && pwd)"
INSTRUQT_DIR="$REPO_ROOT/instruqt"
COURSE_DIR="$REPO_ROOT/course-repo"
PRD="$REPO_ROOT/PRD.md"
DIAGRAMS_DIR="$REPO_ROOT/diagrams"
DOCS_DIR="$TOP_ROOT/docs"
AGENTS="$TOP_ROOT/AGENTS.md"

# Build a list of dirs/files that actually exist, so the grep scope adjusts
# automatically if any of these surfaces are added/removed later.
SCAN_PATHS=()
for p in "$INSTRUQT_DIR" "$COURSE_DIR" "$DIAGRAMS_DIR" "$DOCS_DIR" "$PRD" "$AGENTS"; do
  [ -e "$p" ] && SCAN_PATHS+=("$p")
done

FAIL=0

# ---------------------------------------------------------------------------
echo "=== 1. Banned messaging phrases ==="
# Scope covers learner-facing surfaces (assignment.md, track.yml, PRD),
# diagrams (SVG), externally-hostable demo pages (HTML), and agent-instruction
# files (AGENTS.md). Python code is excluded - its docstrings and comments
# are reviewed module-by-module rather than grep-gated.
BANNED='Skip the [Ww]orkflow|\b(3|11) events\b|events vs\.? [0-9]+|\b50% cheaper\b|half the actions|Compare the cost|workflow scaffolding|[Ww]ithout a [Ww]orkflow|costs less than'
HITS=$(grep -rnE \
  --include='*.md' --include='*.yml' --include='*.svg' --include='*.html' \
  "$BANNED" "${SCAN_PATHS[@]}" 2>/dev/null || true)
if [ -n "$HITS" ]; then
  echo "FAIL banned phrases found:"
  echo "$HITS"
  FAIL=1
else
  echo "OK no banned phrases"
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 2. Competitor product names ==="
# AGENTS.md is excluded - it's the file that DEFINES the no-competitor-names
# rule for human/agent readers, so it has to be allowed to list the names
# (as examples of what NOT to do). Every other file is fair game.
COMPETITORS='\b(Celery|Sidekiq|Sidekick|Faktory|Factory|BullMQ|Resque)\b'
COMPETITOR_SCAN_PATHS=()
for p in "${SCAN_PATHS[@]}"; do
  [ "$p" = "$AGENTS" ] || COMPETITOR_SCAN_PATHS+=("$p")
done
HITS=$(grep -rnE \
  --include='*.md' --include='*.py' --include='*.svg' --include='*.html' --include='*.yml' \
  "$COMPETITORS" "${COMPETITOR_SCAN_PATHS[@]}" 2>/dev/null || true)
if [ -n "$HITS" ]; then
  echo "FAIL competitor names found:"
  echo "$HITS"
  FAIL=1
else
  echo "OK no competitor names"
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 3. Slug / directory consistency ==="
SLUG_FAIL=0
for f in "$INSTRUQT_DIR"/[0-9]*/assignment.md; do
  [ -f "$f" ] || continue
  dir=$(dirname "$f")
  base=$(basename "$dir")
  expected_slug="${base#[0-9][0-9]-}"
  actual_slug=$(awk '/^slug:/ {print $2; exit}' "$f")
  if [ "$expected_slug" != "$actual_slug" ]; then
    echo "FAIL $base/assignment.md slug='$actual_slug' (expected '$expected_slug')"
    SLUG_FAIL=1
    FAIL=1
  fi
done
[ $SLUG_FAIL -eq 0 ] && echo "OK all slugs match dir names"

# ---------------------------------------------------------------------------
echo ""
echo "=== 4. Module 06 same-code invariant ==="
MOD06="$COURSE_DIR/solution/06-same-code-runs-anywhere/src/webhooks"
EXPECTED='from .activities import deliver_webhook'
INVARIANT_FAIL=0
for path in "$MOD06/send_standalone.py" "$MOD06/workflow.py"; do
  if [ ! -f "$path" ]; then
    echo "FAIL $path missing"
    INVARIANT_FAIL=1
    continue
  fi
  if ! grep -qF "$EXPECTED" "$path"; then
    echo "FAIL $path does not import deliver_webhook from .activities"
    INVARIANT_FAIL=1
  fi
done
if [ $INVARIANT_FAIL -eq 0 ]; then
  echo "OK module 06 callers share the same Activity import"
else
  FAIL=1
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 5. Standalone Activities UI over-claims ==="
# The Standalone Activities tab shows a single record. While an Activity is
# still Running it surfaces status, a climbing attempt counter, and the last
# failure; once it is Completed the record does NOT reveal that it was retried
# - no attempt history, no app logs. Retries are observable in the Worker
# console logs and in the receiver, not on a finished Activity. This over-claim
# has regressed twice in Module 02, so it is grep-gated.
# Patterns are deliberately narrow to avoid the legitimate negated framing
# ("not a per-attempt breakdown"), clicking the record itself ("click into
# `deliver-...`"), and the CLI `jq` example (`attempt=1`, no spaces):
#   - "retry history"            : pure over-claim, never legitimate here
#   - "click(ing) into ...attempt": implies a per-attempt drill-in view
#   - "attempt = N" (with spaces): a specific value the UI may not show
#     (matches the codified rule's own phrasing; the CLI `attempt=1` has no
#     spaces and is intentionally not matched)
#   - "count shows it ... retried": claims a finished Activity's counter proves
#     a retry; it does not - that is the exact phrasing that regressed twice
UI_OVERCLAIM='retry history|[Cc]lick(ing)? into[^.]*attempt|[Aa]ttempt = [0-9]|count shows it (was )?retried'
HITS=$(grep -rnE \
  --include='*.md' \
  "$UI_OVERCLAIM" "$INSTRUQT_DIR" 2>/dev/null || true)
if [ -n "$HITS" ]; then
  echo "FAIL Standalone Activities UI over-claim found:"
  echo "$HITS"
  echo "  -> The tab shows one record + an attempt counter. Describe status,"
  echo "     attempt count, and last failure; do not claim a per-attempt"
  echo "     history/drill-in or a specific 'attempt = N' value."
  FAIL=1
else
  echo "OK no Standalone Activities UI over-claims"
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 6. Em dashes ==="
# Team style: no em dashes in learner-facing assignment copy. Use a comma,
# colon, semicolon, or period instead. (Codified after review feedback.)
EMDASH_HITS=$(grep -rn $'—' --include='*.md' "$INSTRUQT_DIR" 2>/dev/null || true)
if [ -n "$EMDASH_HITS" ]; then
  echo "FAIL em dash found (replace it with , : ; or .):"
  echo "$EMDASH_HITS"
  FAIL=1
else
  echo "OK no em dashes"
fi

# ---------------------------------------------------------------------------
echo ""
if [ $FAIL -eq 0 ]; then
  echo "All content checks passed."
else
  echo "Content checks FAILED. See output above."
fi
exit $FAIL
