#!/usr/bin/env bash
# Used in Module 02 to simulate a worker crash mid-activity.
#
# SIGKILL (-9), not the default SIGTERM. The Temporal Python SDK + a sync
# activity running in a ThreadPoolExecutor will gracefully finish its in-flight
# sleep on SIGTERM, which lets the activity complete cleanly - no retry,
# no chaos demo. SIGKILL terminates immediately; the orphaned attempt then
# times out at start_to_close_timeout and Temporal retries it.
pkill -9 -f "python -m webhooks.worker" || true
echo "Worker killed (SIGKILL)."
