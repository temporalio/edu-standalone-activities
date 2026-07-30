#!/usr/bin/env bash
# Clean slate for Module 05's batch sections.
#
# sendbatch submits a fixed Activity ID (deliver-batch-10, or
# deliver-batch-10-<label>). The default ActivityIDConflictPolicy is FAIL, so
# submitting while a previous execution of the same ID is still in flight is
# rejected with ActivityExecutionAlreadyStarted and delivers nothing at all.
# Terminate any leftovers first, then clear the receiver so the counts you read
# belong to this run only.
set -uo pipefail

# Terminating a closed (or never-created) Activity is a harmless no-op.
for id in deliver-batch-10 deliver-batch-10-fixed; do
  temporal activity terminate --activity-id "$id" \
    --reason "module 05 rerun" >/dev/null 2>&1 || true
done

curl -fsS -X POST http://localhost:9000/_reset >/dev/null || true
echo "Terminated any in-flight batch Activities and reset the receiver."
