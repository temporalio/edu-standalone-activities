#!/usr/bin/env bash
# Wait until N deliveries have landed at the receiver, then kill the Worker.
#
# Why not `sleep 4 && kill-worker.sh`: `gradle -q execute` has to start the Gradle
# daemon, configure the build, and fork a JVM before SendBatch submits anything.
# That regularly takes longer than 4 seconds, so a fixed sleep kills the Worker
# before a single item is delivered and the demo shows zero progress instead of N.
# Polling the receiver makes the kill land mid-batch no matter how slow the start is.
set -uo pipefail

TARGET="${1:-4}"
TIMEOUT="${2:-90}"

delivered() {
  curl -fsS http://localhost:9000/_received 2>/dev/null | jq -r '.processed_count // 0'
}

echo "Waiting for $TARGET deliveries to land before killing the Worker..."
for _ in $(seq 1 "$TIMEOUT"); do
  n="$(delivered)"
  if [ -n "$n" ] && [ "$n" -ge "$TARGET" ]; then
    break
  fi
  sleep 1
done

echo "Receiver has $(delivered) deliveries. Killing the Worker now."
exec "$(dirname "$0")/kill-worker.sh"
