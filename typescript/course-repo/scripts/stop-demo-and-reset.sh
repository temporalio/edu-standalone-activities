#!/usr/bin/env bash
# Terminate any leftover demo-* Standalone Activities from the rate-limit demo,
# then reset the receiver so the next run starts from zero.
set -uo pipefail

for i in $(seq 0 59); do
  temporal activity terminate --activity-id "$(printf 'demo-%03d' "$i")" \
    --reason "rate-limit demo cleanup" >/dev/null 2>&1 || true
done

curl -fsS -X POST http://localhost:9000/_reset >/dev/null || true
echo "Stopped any leftover demo Activities and reset the receiver."
