#!/usr/bin/env bash
# Restart the worker from the caller's cwd (must be a module's exercise dir).
nohup ts-node src/worker.ts > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
