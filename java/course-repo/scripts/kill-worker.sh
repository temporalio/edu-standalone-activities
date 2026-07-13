#!/usr/bin/env bash
# SIGKILL the running Worker so an in-flight Activity attempt dies mid-execution.
# The Worker runs via `mvn exec:java -Dexec.mainClass=webhooks.Worker`, so that
# string is on the process command line and pkill -f can match it.
pkill -9 -f "exec.mainClass=webhooks.Worker" 2>/dev/null \
  || pkill -9 -f "webhooks.Worker" 2>/dev/null \
  || true
echo "Worker service down."
