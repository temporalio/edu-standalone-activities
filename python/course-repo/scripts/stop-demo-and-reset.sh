#!/usr/bin/env bash
# Reset for Section 4's capped run.
#
# Section 2's pain demo (send_bulk_demo) fans out demo-* Standalone Activities
# that retry on every 429. While they drain they generate hundreds of retries,
# and if any are still in flight they keep hitting the receiver and inflate the
# counts you check in this section. Terminate any leftover demo-* Activities,
# then clear the receiver so this run starts from a clean zero.
set -uo pipefail

# send_bulk_demo uses IDs demo-000 .. demo-059. Terminating an Activity that has
# already completed (or never existed) is a harmless no-op, so just sweep the range.
for i in $(seq 0 59); do
  temporal activity terminate --activity-id "$(printf 'demo-%03d' "$i")" \
    --reason "rate-limit demo cleanup" >/dev/null 2>&1 || true
done

curl -fsS -X POST http://localhost:9000/_reset >/dev/null || true
echo "Stopped any leftover demo Activities and reset the receiver."
