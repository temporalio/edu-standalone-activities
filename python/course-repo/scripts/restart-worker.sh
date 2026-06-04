#!/usr/bin/env bash
# Restart the worker in the background from the caller's cwd (must be a
# module's exercise dir, where pyproject.toml lives). Logs to /tmp/worker.log.
nohup uv run python -m webhooks.worker > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
