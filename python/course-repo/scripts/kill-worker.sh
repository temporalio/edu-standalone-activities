#!/usr/bin/env bash
# Used in Module 02 (and beyond) to simulate worker crashes mid-activity.
pkill -f "python -m webhooks.worker" || true
echo "Worker killed."
