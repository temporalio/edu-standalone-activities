#!/usr/bin/env bash
curl -fsS -X POST http://localhost:9000/_reset > /dev/null
echo "Webhook receiver reset."
