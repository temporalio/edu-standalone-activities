#!/usr/bin/env bash
# SIGKILL (not TERM) to guarantee a mid-flight crash for the retry/heartbeat demos.
# `gradle ... execute -PmainClass=webhook.Worker` forks a child JVM (java ... webhook.Worker).
# Match both the Gradle launcher (carries -PmainClass=webhook.Worker) and the forked JVM
# (carries webhook.Worker as its main class). Every pattern is best-effort and NOT
# short-circuited with `||`, so both processes die regardless of which one holds the Activity.
pkill -9 -f "mainClass=webhook.Worker" 2>/dev/null
pkill -9 -f "webhook.Worker" 2>/dev/null
true
echo "Worker killed."
