#!/usr/bin/env bash
# Kills the running worker(s). Used by tests and any module that wants a
# clean restart. The pattern is "webhooks.worker" (without the python prefix)
# because uv launches the process as `python3 -m webhooks.worker` and the
# python-vs-python3 difference breaks more specific patterns.
pkill -9 -f "webhooks.worker" || true
echo "Worker killed."
