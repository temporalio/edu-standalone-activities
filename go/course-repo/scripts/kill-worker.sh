#!/usr/bin/env bash
# SIGKILL (not TERM) to guarantee a mid-flight crash for the retry/heartbeat demos.
# `go run ./worker` is a wrapper process that spawns a SEPARATE compiled-binary
# child process. Killing only the wrapper leaves that child running and
# orphaned (verified in-container: with a warm build cache, `go run` execs the
# binary straight out of $GOCACHE/go-build/<hash>/worker with no "exe/worker"
# path segment at all, so the old first pattern never matched, and the old
# `||` chain then stopped at the first successful kill (the wrapper) and
# never reached a pattern that would catch the compiled child).
# Every pattern below is best-effort and intentionally NOT short-circuited
# with `||`: run them all so both the wrapper and the compiled child die
# regardless of which path shape the current toolchain/cache state produces.
pkill -9 -f "go run ./worker" 2>/dev/null
pkill -9 -f "exe/worker" 2>/dev/null
pkill -9 -f "go-build.*/worker$" 2>/dev/null
pkill -9 -f "/worker$" 2>/dev/null
true
echo "Worker killed."
