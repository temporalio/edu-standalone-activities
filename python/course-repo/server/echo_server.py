"""Stdlib HTTP echo server used as the webhook receiver in this tutorial.

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
        if self.path in ("/", "/_received"):
            self._handle_received()
        else:
            self._respond(404, {"error": "not found"})

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
        body = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main() -> None:
    server = HTTPServer(("0.0.0.0", 9000), EchoHandler)
    print("Echo server listening on :9000")
    server.serve_forever()


if __name__ == "__main__":
    main()
