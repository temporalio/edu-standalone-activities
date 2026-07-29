#!/usr/bin/env bash
# Clean up before the rate-capped run in Module 04, section 4.
#
# Section 2 runs SendBulkDemo, which fans out demo-* Standalone Activities against a
# receiver capped at 2 req/sec. Those Activities retry on every 429, so if any are still
# draining they keep hitting the receiver and inflate the delivery counts you are asked to
# check in section 4. Terminate any leftover demo-* Activities, then clear the receiver so
# the capped run starts from zero.
set -uo pipefail

# SendBulkDemo uses IDs demo-000 .. demo-059. Terminating an Activity that already completed
# (or never existed) is a harmless no-op, so just sweep the range.
for i in $(seq 0 59); do
  temporal activity terminate --activity-id "$(printf 'demo-%03d' "$i")" \
    --reason "rate-limit demo cleanup" >/dev/null 2>&1 || true
done

curl -fsS -X POST http://localhost:9000/_reset >/dev/null || true
echo "Stopped any leftover demo Activities and reset the receiver."
