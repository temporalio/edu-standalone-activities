#!/usr/bin/env bash
# SIGKILL the Worker JVM so an in-flight Activity dies mid-attempt (needed for the
# crash/heartbeat demos). SIGTERM would let the SDK drain gracefully and no retry fires.
pkill -9 -f "mainClass=webhook.Worker" 2>/dev/null || true
pkill -9 -f "webhook.Worker" 2>/dev/null || true
echo "Worker killed."
