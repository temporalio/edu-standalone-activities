#!/usr/bin/env bash
# SIGKILL (not TERM) to guarantee a mid-flight crash for the retry/heartbeat demos.
# `go run ./worker` compiles to a temp binary under $GOCACHE/go-build.../exe/worker,
# so match that path first and fall back to broader patterns.
pkill -9 -f "exe/worker" 2>/dev/null || pkill -9 -f "go run ./worker" 2>/dev/null || pkill -9 -f "/worker$" 2>/dev/null || true
echo "Worker killed."
