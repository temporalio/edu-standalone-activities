"""Stdlib HTTP webhook receiver used as the webhook receiver in this tutorial.

Endpoints:
  POST /hooks      - Receives a webhook delivery. Records it. Returns 200.
                     Honors an optional Idempotency-Key header (used by Module 02).
  GET  /_received  - Returns {"count": N, "deliveries": [...]} for inspection.
  POST /_reset     - Clears recorded state. Used between checks.

No external dependencies - intentionally stdlib-only so it works in any sandbox.
"""

import json
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, HTTPServer

_received: list[dict] = []
_lock = threading.Lock()


class EchoHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        return  # silence default access logs

    def do_POST(self) -> None:
        if self.path == "/hooks":
            self._handle_hook()
        elif self.path == "/_reset":
            self._handle_reset()
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
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length > 0 else b"{}"
        try:
            body = json.loads(raw)
        except json.JSONDecodeError:
            self._respond(400, {"error": "invalid JSON"})
            return

        idem_key = self.headers.get("Idempotency-Key")

        with _lock:
            # Idempotency dedup (exercised in Module 02; harmless in Module 01).
            if idem_key:
                for prior in _received:
                    if prior.get("idempotency_key") == idem_key:
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
                "count": len(_received),
                "deliveries": list(_received),
            })

    def _handle_reset(self) -> None:
        with _lock:
            _received.clear()
        self._respond(200, {"status": "reset"})

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
