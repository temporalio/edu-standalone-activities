#!/usr/bin/env bash
pkill -9 -f "ts-node.*worker.ts" 2>/dev/null || pkill -9 -f "worker.ts" 2>/dev/null || true
echo "Worker killed."
