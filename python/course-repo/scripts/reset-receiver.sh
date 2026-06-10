#!/usr/bin/env bash
# Clear the webhook receiver's recorded deliveries. Used between runs.
curl -fsS -X POST http://localhost:9000/_reset > /dev/null
echo "Webhook receiver reset."
