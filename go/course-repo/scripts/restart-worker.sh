#!/usr/bin/env bash
# Restart the worker from the caller's cwd (must be a module's exercise dir).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/kill-worker.sh" > /dev/null
nohup go run ./worker > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
