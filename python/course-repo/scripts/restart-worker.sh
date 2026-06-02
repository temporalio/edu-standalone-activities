#!/usr/bin/env bash
# Restart the worker in the background. Logs to /tmp/worker.log.
cd /root/workshop
nohup uv run python -m webhooks.worker > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
