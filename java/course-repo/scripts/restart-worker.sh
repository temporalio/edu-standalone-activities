#!/usr/bin/env bash
# Restart the Worker from the caller's cwd (must be a module directory).
nohup gradle -q execute -PmainClass=webhook.Worker > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
