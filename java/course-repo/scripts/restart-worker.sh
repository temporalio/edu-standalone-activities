#!/usr/bin/env bash
# Restart the Worker from the caller's cwd (must be a module's exercise or
# solution dir, where the pom.xml lives). Logs to /tmp/worker.log.
nohup mvn -q compile exec:java -Dexec.mainClass=webhooks.Worker > /tmp/worker.log 2>&1 &
echo "Worker restarted (logs: /tmp/worker.log)"
