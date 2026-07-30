#!/usr/bin/env bash
# Set the receiver's rate-limit cap and verify it actually took effect.
#
# Why the read-back: the cap is server-side state on the receiver, and a challenge
# setup script clears it to 0 on entry. If the cap silently isn't applied, the
# rate-limit demo just succeeds and looks like rate limiting does nothing, which is
# very confusing. Failing loudly here is better than a demo that quietly works.
set -uo pipefail

LIMIT="${1:-2}"

curl -fsS -X POST "http://localhost:9000/_rate_limit?limit=${LIMIT}" > /dev/null || {
  echo "ERROR: could not reach the webhook receiver on :9000. Is it running?" >&2
  exit 1
}

ACTUAL="$(curl -fsS http://localhost:9000/_received 2>/dev/null | jq -r '.rate_limit // "unknown"')"

if [ "$ACTUAL" != "$LIMIT" ]; then
  echo "ERROR: asked the receiver for a cap of ${LIMIT} req/sec but it reports ${ACTUAL}." >&2
  echo "Re-run this command. If it keeps failing, the receiver may have restarted." >&2
  exit 1
fi

echo "Receiver rate limit is now ${ACTUAL} req/sec."
