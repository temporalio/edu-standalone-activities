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
#
# Run locally before `instruqt track push` or git push.
# Exit 0 = clean, non-zero = problems found.

set -uo pipefail  # no -e: we want to run every check and report all failures.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INSTRUQT_DIR="$REPO_ROOT/instruqt"
COURSE_DIR="$REPO_ROOT/course-repo"
PRD="$REPO_ROOT/PRD.md"

FAIL=0

# ---------------------------------------------------------------------------
echo "=== 1. Banned messaging phrases ==="
BANNED='Skip the [Ww]orkflow|\b(3|11) events\b|events vs\.? [0-9]+|\b50% cheaper\b|half the actions|Compare the cost|workflow scaffolding|[Ww]ithout a [Ww]orkflow|costs less than'
HITS=$(grep -rnE --include='*.md' --include='*.yml' "$BANNED" "$INSTRUQT_DIR" "$PRD" 2>/dev/null || true)
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
COMPETITORS='\b(Celery|Sidekiq|Sidekick|Faktory|Factory|BullMQ|Resque)\b'
HITS=$(grep -rnE --include='*.md' --include='*.py' "$COMPETITORS" "$INSTRUQT_DIR" "$COURSE_DIR" 2>/dev/null || true)
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
if [ $FAIL -eq 0 ]; then
  echo "All content checks passed."
else
  echo "Content checks FAILED. See output above."
fi
exit $FAIL
