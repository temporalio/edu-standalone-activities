#!/usr/bin/env bash
# Restart the Worker from the caller's cwd (must be a module's exercise/solution dir).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"$SCRIPT_DIR/kill-worker.sh" > /dev/null
nohup gradle -q --console=plain execute -PmainClass=webhook.Worker > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
