"""Stdlib HTTP webhook receiver for the Standalone Activities tutorial.

SDK-agnostic on purpose: it is plain Python 3 with no third-party
dependencies, so the same receiver backs the Python, TypeScript, Go, and Java
editions of this course. The Java sandbox runs it on port 9000.

Endpoints:
  POST /hooks       Records a webhook delivery and returns 200. Honors an
                    optional Idempotency-Key header (Module 02): a repeated key
                    is counted as deduped, not processed again. Returns 429 when
                    rate limiting is on and the caller exceeds the cap (Module 04).
  POST /_reset      Clears recorded deliveries and counters. Also clears the
                    rate-limit setting.
  POST /_rate_limit Sets the cap from ?limit=N (requests/sec). limit=0 disables.
  GET  /_received   Returns the counters and recorded deliveries as JSON.
  GET  /            Auto-refreshing HTML dashboard of the counters.
"""

import json
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

PORT = 9000

_lock = threading.Lock()


class State:
    def __init__(self):
        self.reset(clear_rate_limit=True)

    def reset(self, clear_rate_limit=False):
        self.received_count = 0
        self.processed_count = 0
        self.deduped_count = 0
        self.throttled_count = 0
        self.seen_keys = set()
        self.deliveries = []
        self.window_timestamps = []
        if clear_rate_limit:
            self.rate_limit = 0


state = State()


def is_rate_limited():
    """Sliding-window limiter: at most `rate_limit` requests per rolling second."""
    if state.rate_limit == 0:
        return False
    now = time.time()
    cutoff = now - 1.0
    state.window_timestamps = [t for t in state.window_timestamps if t > cutoff]
    if len(state.window_timestamps) >= state.rate_limit:
        return True
    state.window_timestamps.append(now)
    return False


def stats_json():
    return {
        "received_count": state.received_count,
        "processed_count": state.processed_count,
        "deduped_count": state.deduped_count,
        "throttled_count": state.throttled_count,
        "rate_limit": state.rate_limit,
        # `count` mirrors processed_count for callers that expect a single number.
        "count": state.processed_count,
        "deliveries": state.deliveries,
    }


HTML_PAGE = """<!DOCTYPE html>
<html><head><meta charset="utf-8">
<meta http-equiv="refresh" content="2">
<title>Webhook Receiver</title>
<style>
body { font-family: monospace; background: #1a1a2e; color: #e2e8f0; padding: 2rem; }
h1 { color: #7aa2ff; }
.stat { display: inline-block; margin: 0.5rem 1rem 0.5rem 0; padding: 0.5rem 1rem;
        background: #252540; border-radius: 4px; }
.stat .label { color: #a0aec0; font-size: 0.85em; }
.stat .value { font-size: 1.8em; font-weight: bold; color: #9ae6b4; }
.stat .value.red { color: #fc8181; }
pre { background: #252540; padding: 1rem; border-radius: 4px; overflow-x: auto; }
</style>
</head>
<body>
<h1>Webhook Receiver</h1>
<p style="color:#a0aec0">Auto-refreshes every 2 seconds.</p>
<div id="stats"></div>
<pre id="json"></pre>
<script>
fetch('/_received').then(r=>r.json()).then(d=>{
  document.getElementById('json').textContent = JSON.stringify(d, null, 2);
  const stats = document.getElementById('stats');
  const items = [
    ['Received', d.received_count, false],
    ['Processed', d.processed_count, false],
    ['Deduped', d.deduped_count, false],
    ['Throttled (429)', d.throttled_count, d.throttled_count > 0],
    ['Rate limit (req/s)', d.rate_limit || 'off', false],
  ];
  stats.innerHTML = items.map(([l,v,r]) =>
    '<div class="stat"><div class="label">'+l+'</div><div class="value'+(r?' red':'')+'">'
    +v+'</div></div>').join('');
});
</script>
</body></html>"""


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):
        pass  # keep the sandbox log quiet

    def _send(self, code, obj=None, html=None):
        self.send_response(code)
        if html is not None:
            self.send_header("Content-Type", "text/html")
            body = html.encode()
        else:
            self.send_header("Content-Type", "application/json")
            body = json.dumps(obj).encode()
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _read_body(self):
        length = int(self.headers.get("Content-Length", 0) or 0)
        raw = self.rfile.read(length) if length else b""
        try:
            return json.loads(raw.decode() or "{}")
        except Exception:
            return {}

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/_received":
            with _lock:
                self._send(200, stats_json())
            return
        if path in ("/", ""):
            self._send(200, html=HTML_PAGE)
            return
        self._send(404, {"error": "not found"})

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path

        if path == "/hooks":
            with _lock:
                state.received_count += 1
                if is_rate_limited():
                    state.throttled_count += 1
                    self._send(429, {"error": "Too Many Requests"})
                    return
                body = self._read_body()
                key = self.headers.get("Idempotency-Key")
                if key is not None and key in state.seen_keys:
                    state.deduped_count += 1
                    self._send(200, {"deduped": True, "idempotencyKey": key})
                    return
                if key is not None:
                    state.seen_keys.add(key)
                state.processed_count += 1
                state.deliveries.append({
                    "received_at": datetime.now(timezone.utc).isoformat(),
                    "idempotency_key": key,
                    "body": body,
                })
                self._send(200, {"ok": True, "processed": state.processed_count})
            return

        if path == "/_reset":
            with _lock:
                state.reset(clear_rate_limit=True)
            self._send(200, {"reset": True})
            return

        if path == "/_rate_limit":
            qs = parse_qs(parsed.query)
            try:
                limit = int(qs.get("limit", ["0"])[0])
            except ValueError:
                limit = 0
            with _lock:
                state.rate_limit = max(0, limit)
                state.window_timestamps = []
            self._send(200, {"rateLimit": state.rate_limit})
            return

        self._send(404, {"error": "not found"})


def main():
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Webhook receiver listening on :{PORT}")
    server.serve_forever()


if __name__ == "__main__":
    main()
