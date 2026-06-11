"""Stdlib HTTP webhook receiver used as the webhook receiver in this tutorial.

Endpoints:
  POST /hooks       - Receives a webhook delivery. Records it. Returns 200.
                      Honors an optional Idempotency-Key header (used by Module 02).
                      Returns 429 when the rate-limit mode is enabled and the
                      caller exceeds the configured cap (used by Module 04).
  GET  /_received   - Returns request and processed-delivery counts plus the
                      current rate-limit setting and throttled-request count.
  POST /_reset      - Clears recorded state and throttled count. Preserves the
                      rate-limit setting so a lesson can reset between checks
                      without losing the configured cap.
  POST /_rate_limit - Sets the rate-limit cap from `?limit=N` (requests/sec).
                      `limit=0` disables rate limiting. Token bucket model:
                      capacity = N tokens, refill = N tokens/sec.

No external dependencies - intentionally stdlib-only so it works in any sandbox.
"""

import json
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import parse_qs, urlparse

_received: list[dict] = []
_request_count = 0
_deduped_count = 0
_throttled_count = 0
# Rate-limit state. 0 means disabled; >0 means N requests/sec via token bucket.
_rate_limit = 0
_tokens = 0.0
_last_refill = time.monotonic()
_lock = threading.Lock()


def _try_consume_token_locked() -> bool:
    """Return True if a token was consumed (request allowed), False if rate-limited.

    Caller must hold _lock. Token bucket: capacity = _rate_limit, refill rate =
    _rate_limit tokens/sec. A burst of size _rate_limit can pass at once; after
    that, callers wait for refill.
    """
    global _tokens, _last_refill
    if _rate_limit == 0:
        return True
    now = time.monotonic()
    elapsed = now - _last_refill
    _tokens = min(float(_rate_limit), _tokens + elapsed * _rate_limit)
    _last_refill = now
    if _tokens >= 1.0:
        _tokens -= 1.0
        return True
    return False


class EchoHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        return  # silence default access logs

    def do_POST(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/hooks":
            self._handle_hook()
        elif parsed.path == "/_reset":
            self._handle_reset()
        elif parsed.path == "/_rate_limit":
            self._handle_rate_limit(parsed.query)
        else:
            self._respond(404, {"error": "not found"})

    def do_GET(self) -> None:
        if self.path == "/":
            self._handle_dashboard()
        elif self.path == "/_received":
            self._handle_received()
        else:
            self._respond(404, {"error": "not found"})

    def _handle_dashboard(self) -> None:
        """Auto-refreshing HTML view of /_received. Polls every 2s via fetch."""
        html = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Webhook receiver</title>
<style>
  body { background: #1a1a2e; color: #e2e8f0; font-family: ui-monospace, "SF Mono", monospace; padding: 1em; margin: 0; }
  header { color: #b794f6; font-weight: 600; margin-bottom: 0.6em; }
  pre { white-space: pre-wrap; margin: 0; font-size: 13px; line-height: 1.45; }
  .meta { color: #a0aec0; font-size: 11px; margin-top: 0.4em; }
</style>
</head>
<body>
<header>Webhook receiver &mdash; live</header>
<pre id="data">loading...</pre>
<div class="meta">auto-refreshes every 2s</div>
<script>
async function refresh() {
  try {
    const r = await fetch('/_received');
    const j = await r.json();
    document.getElementById('data').textContent = JSON.stringify(j, null, 2);
  } catch (e) {
    document.getElementById('data').textContent = 'error: ' + e;
  }
}
refresh();
setInterval(refresh, 2000);
</script>
</body>
</html>
"""
        body = html.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _handle_hook(self) -> None:
        global _request_count, _deduped_count, _throttled_count

        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length > 0 else b"{}"
        try:
            body = json.loads(raw)
        except json.JSONDecodeError:
            self._respond(400, {"error": "invalid JSON"})
            return

        idem_key = self.headers.get("Idempotency-Key")

        with _lock:
            _request_count += 1

            # Rate-limit mode (exercised in Module 04). Counts the inbound
            # attempt, then 429s if the bucket is empty. The Activity sees
            # a retryable HTTP error and Temporal retries on its policy.
            if not _try_consume_token_locked():
                _throttled_count += 1
                self._respond(429, {"status": "rate_limited"})
                return

            # Idempotency dedup (exercised in Module 02; harmless in Module 01).
            if idem_key:
                for prior in _received:
                    if prior.get("idempotency_key") == idem_key:
                        _deduped_count += 1
                        self._respond(200, {"status": "ok", "deduped": True})
                        return

            _received.append({
                "received_at": datetime.now(timezone.utc).isoformat(),
                "body": body,
                "idempotency_key": idem_key,
            })

        self._respond(200, {"status": "ok", "deduped": False})

    def _handle_received(self) -> None:
        with _lock:
            self._respond(200, {
                "received_count": _request_count,
                "processed_count": len(_received),
                "deduped_count": _deduped_count,
                "throttled_count": _throttled_count,
                "rate_limit": _rate_limit,
                # Backward-compatible alias used by the lessons.
                "count": len(_received),
                "deliveries": list(_received),
            })

    def _handle_reset(self) -> None:
        global _request_count, _deduped_count, _throttled_count

        with _lock:
            _received.clear()
            _request_count = 0
            _deduped_count = 0
            _throttled_count = 0
            # NB: rate-limit setting persists across resets so a lesson can
            # zero counters between observations without losing the cap.
        self._respond(200, {"status": "reset"})

    def _handle_rate_limit(self, query: str) -> None:
        global _rate_limit, _tokens, _last_refill

        params = parse_qs(query or "")
        raw_limit = params.get("limit", ["0"])[0]
        try:
            limit = int(raw_limit)
        except ValueError:
            self._respond(400, {"error": "limit must be an integer"})
            return
        if limit < 0:
            self._respond(400, {"error": "limit must be >= 0"})
            return

        with _lock:
            _rate_limit = limit
            # Refill the bucket on enable so the first burst can pass cleanly;
            # otherwise an empty bucket from prior state would 429 the first N.
            _tokens = float(limit)
            _last_refill = time.monotonic()

        self._respond(200, {"status": "ok", "rate_limit": limit})

    def _respond(self, code: int, payload: dict) -> None:
        # indent=2 so the echo tab is readable without toggling Pretty-print.
        body = json.dumps(payload, indent=2).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main() -> None:
    server = HTTPServer(("0.0.0.0", 9000), EchoHandler)
    print("Webhook receiver listening on :9000")
    server.serve_forever()


if __name__ == "__main__":
    main()
