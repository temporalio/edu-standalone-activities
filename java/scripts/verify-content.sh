#!/usr/bin/env bash
# Content guardrails for the SAA tutorial (Java track).
#
# Hard-fails the build if any of these slip back in:
#   - Banned messaging phrases (SAA-vs-Workflow framing, event-count math,
#     cost-comparison vocabulary)
#   - Specific competitor product names in learner-facing copy
#   - Slug / directory name mismatches in Instruqt assignment frontmatter
#   - Module 06's "same Activity, two callers" invariant (both the standalone
#     caller and the Workflow caller must reference the SAME deliverWebhook
#     Activity from the webhook package)
#   - Standalone Activities UI over-claims (the tab shows one record + an
#     attempt counter, NOT a per-attempt "retry history", per-attempt
#     drill-in, or a specific "attempt = N" value)
#   - Em dashes in learner-facing assignment copy
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

SCAN_PATHS=()
for p in "$INSTRUQT_DIR" "$COURSE_DIR" "$DIAGRAMS_DIR" "$DOCS_DIR" "$PRD" "$AGENTS"; do
  [ -e "$p" ] && SCAN_PATHS+=("$p")
done

FAIL=0

# ---------------------------------------------------------------------------
echo "=== 1. Banned messaging phrases ==="
BANNED='Skip the [Ww]orkflow|\b(3|11) events\b|events vs\.? [0-9]+|\b50% cheaper\b|half the actions|Compare the cost|workflow scaffolding|[Ww]ithout a [Ww]orkflow|costs less than'
HITS=$(grep -rnE \
  --include='*.md' --include='*.yml' --include='*.svg' --include='*.html' \
  "$BANNED" "${SCAN_PATHS[@]}" 2>/dev/null || true)
if [ -n "$HITS" ]; then
  echo "FAIL banned phrases found:"; echo "$HITS"; FAIL=1
else
  echo "OK no banned phrases"
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 2. Competitor product names ==="
COMPETITORS='\b(Celery|Sidekiq|Sidekick|Faktory|Factory|BullMQ|Resque)\b'
COMPETITOR_SCAN_PATHS=()
for p in "${SCAN_PATHS[@]}"; do
  [ "$p" = "$AGENTS" ] || COMPETITOR_SCAN_PATHS+=("$p")
done
HITS=$(grep -rnE \
  --include='*.md' --include='*.java' --include='*.svg' --include='*.html' --include='*.yml' \
  "$COMPETITORS" "${COMPETITOR_SCAN_PATHS[@]}" 2>/dev/null || true)
if [ -n "$HITS" ]; then
  echo "FAIL competitor names found:"; echo "$HITS"; FAIL=1
else
  echo "OK no competitor names"
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 3. Slug / directory consistency ==="
SLUG_FAIL=0
for f in "$INSTRUQT_DIR"/[0-9]*/assignment.md; do
  [ -f "$f" ] || continue
  dir=$(dirname "$f"); base=$(basename "$dir")
  expected_slug="${base#[0-9][0-9]-}"
  actual_slug=$(awk '/^slug:/ {print $2; exit}' "$f")
  if [ "$expected_slug" != "$actual_slug" ]; then
    echo "FAIL $base/assignment.md slug='$actual_slug' (expected '$expected_slug')"; SLUG_FAIL=1; FAIL=1
  fi
done
[ $SLUG_FAIL -eq 0 ] && echo "OK all slugs match dir names"

# ---------------------------------------------------------------------------
echo ""
echo "=== 4. Module 06 same-code invariant ==="
# Both the standalone caller (SendStandalone.java) and the Workflow caller
# (WebhookWorkflowImpl.java) must reference the SAME deliverWebhook Activity from
# the WebhookActivities interface. Checked in both exercise and solution.
INVARIANT_FAIL=0
for variant in exercise solution; do
  MOD06="$COURSE_DIR/$variant/06-same-code-runs-anywhere/src/main/java/webhook"
  STANDALONE="$MOD06/SendStandalone.java"
  WORKFLOW="$MOD06/WebhookWorkflowImpl.java"

  if [ ! -f "$STANDALONE" ]; then
    echo "FAIL $STANDALONE missing"; INVARIANT_FAIL=1
  elif ! grep -qE 'deliverWebhook\b' "$STANDALONE"; then
    echo "FAIL $STANDALONE does not reference deliverWebhook"; INVARIANT_FAIL=1
  fi

  if [ ! -f "$WORKFLOW" ]; then
    echo "FAIL $WORKFLOW missing"; INVARIANT_FAIL=1
  else
    if ! grep -qE 'WebhookActivities\.class' "$WORKFLOW"; then
      echo "FAIL $WORKFLOW does not build a stub of WebhookActivities"; INVARIANT_FAIL=1
    fi
    if ! grep -qE '\.deliverWebhook\(' "$WORKFLOW"; then
      echo "FAIL $WORKFLOW does not call .deliverWebhook(...)"; INVARIANT_FAIL=1
    fi
  fi
done
if [ $INVARIANT_FAIL -eq 0 ]; then
  echo "OK module 06 callers (standalone + Workflow) share the same deliverWebhook Activity"
else
  FAIL=1
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 5. Standalone Activities UI over-claims ==="
UI_OVERCLAIM='retry history|[Cc]lick(ing)? into[^.]*attempt|[Aa]ttempt = [0-9]|count shows it (was )?retried'
HITS=$(grep -rnE --include='*.md' "$UI_OVERCLAIM" "$INSTRUQT_DIR" 2>/dev/null || true)
if [ -n "$HITS" ]; then
  echo "FAIL Standalone Activities UI over-claim found:"; echo "$HITS"
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
EMDASH_HITS=$(grep -rn $'—' --include='*.md' "$INSTRUQT_DIR" 2>/dev/null || true)
if [ -n "$EMDASH_HITS" ]; then
  echo "FAIL em dash found (replace it with , : ; or .):"; echo "$EMDASH_HITS"; FAIL=1
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
