#!/usr/bin/env bash
# Clear the echo server's recorded deliveries. Used between runs.
curl -fsS -X POST http://localhost:9000/_reset > /dev/null
echo "Echo server reset."
