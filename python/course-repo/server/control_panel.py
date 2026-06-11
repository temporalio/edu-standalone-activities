"""Control Panel server for adjusting webhook failure rate.

Serves a simple UI with a slider (0-100) that controls how often the webhook
receiver returns errors. The failure rate is stored in a shared file that the
webhook receiver reads on each request.

Endpoints:
  GET  /             - Control Panel HTML UI with slider
  GET  /_failure_rate - Returns {"failure_rate": N} (0-100)
  POST /_failure_rate - Sets the failure rate. Body: {"failure_rate": N}

No external dependencies - intentionally stdlib-only so it works in any sandbox.
"""

import json
from http.server import BaseHTTPRequestHandler, HTTPServer

FAILURE_RATE_FILE = "/tmp/failure_rate"
DEFAULT_FAILURE_RATE = 0


def _read_failure_rate() -> int:
    try:
        with open(FAILURE_RATE_FILE, "r") as f:
            return int(f.read().strip())
    except (FileNotFoundError, ValueError):
        return DEFAULT_FAILURE_RATE


def _write_failure_rate(rate: int) -> None:
    with open(FAILURE_RATE_FILE, "w") as f:
        f.write(str(rate))


class ControlPanelHandler(BaseHTTPRequestHandler):
    def log_message(self, format, *args):
        return  # silence default access logs

    def do_GET(self) -> None:
        if self.path == "/":
            self._handle_dashboard()
        elif self.path == "/_failure_rate":
            self._handle_get_rate()
        else:
            self._respond(404, {"error": "not found"})

    def do_POST(self) -> None:
        if self.path == "/_failure_rate":
            self._handle_set_rate()
        else:
            self._respond(404, {"error": "not found"})

    def _handle_dashboard(self) -> None:
        rate = _read_failure_rate()
        html = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Control Panel</title>
<style>
  body {{
    background: #1a1a2e;
    color: #e2e8f0;
    font-family: system-ui, -apple-system, 'Segoe UI', sans-serif;
    padding: 2em;
    margin: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 100vh;
  }}
  h1 {{
    color: #b794f6;
    font-size: 1.5em;
    margin-bottom: 0.3em;
  }}
  .subtitle {{
    color: #a0aec0;
    font-size: 0.9em;
    margin-bottom: 2em;
  }}
  .slider-container {{
    background: #2d3748;
    border: 1px solid #4a5568;
    border-radius: 8px;
    padding: 2em 3em;
    text-align: center;
    width: 100%;
    max-width: 500px;
  }}
  .slider-label {{
    font-size: 0.95em;
    margin-bottom: 1em;
    color: #e2e8f0;
  }}
  input[type="range"] {{
    -webkit-appearance: none;
    appearance: none;
    width: 100%;
    height: 8px;
    border-radius: 4px;
    background: #4a5568;
    outline: none;
    margin: 1em 0;
  }}
  input[type="range"]::-webkit-slider-thumb {{
    -webkit-appearance: none;
    appearance: none;
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: #b794f6;
    cursor: pointer;
    border: 2px solid #1a1a2e;
    box-shadow: 0 0 8px rgba(183, 148, 246, 0.4);
  }}
  input[type="range"]::-moz-range-thumb {{
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: #b794f6;
    cursor: pointer;
    border: 2px solid #1a1a2e;
    box-shadow: 0 0 8px rgba(183, 148, 246, 0.4);
  }}
  .rate-display {{
    font-size: 3em;
    font-weight: 700;
    color: #b794f6;
    margin: 0.2em 0;
    font-family: ui-monospace, 'SF Mono', monospace;
  }}
  .rate-suffix {{
    font-size: 0.4em;
    color: #a0aec0;
  }}
  .status {{
    margin-top: 1em;
    font-size: 0.85em;
    color: #a0aec0;
    min-height: 1.2em;
  }}
  .status.success {{ color: #9ae6b4; }}
  .status.error {{ color: #fc8181; }}
  .description {{
    margin-top: 1.5em;
    font-size: 0.85em;
    color: #a0aec0;
    line-height: 1.5;
    max-width: 500px;
    text-align: center;
  }}
</style>
</head>
<body>
<h1>Control Panel</h1>
<div class="subtitle">Adjust how often webhook requests fail</div>

<div class="slider-container">
  <div class="slider-label">Failure Rate</div>
  <div class="rate-display"><span id="rate-value">{rate}</span><span class="rate-suffix">%</span></div>
  <input type="range" id="slider" min="0" max="100" value="{rate}">
  <div class="status" id="status"></div>
</div>

<div class="description">
  <strong>0%</strong> = requests never fail &nbsp;&bull;&nbsp; <strong>100%</strong> = requests always fail<br>
  Slide to 100 to watch Temporal retry the activity. Slide to 0 to let it recover.
</div>

<script>
const slider = document.getElementById('slider');
const display = document.getElementById('rate-value');
const status = document.getElementById('status');

slider.addEventListener('input', () => {{
  display.textContent = slider.value;
}});

let timeout;
slider.addEventListener('change', async () => {{
  const rate = parseInt(slider.value, 10);
  try {{
    const r = await fetch('/_failure_rate', {{
      method: 'POST',
      headers: {{'Content-Type': 'application/json'}},
      body: JSON.stringify({{failure_rate: rate}})
    }});
    if (r.ok) {{
      status.textContent = 'Updated to ' + rate + '%';
      status.className = 'status success';
    }} else {{
      status.textContent = 'Failed to update';
      status.className = 'status error';
    }}
  }} catch (e) {{
    status.textContent = 'Error: ' + e.message;
    status.className = 'status error';
  }}
  clearTimeout(timeout);
  timeout = setTimeout(() => {{ status.textContent = ''; }}, 3000);
}});
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

    def _handle_get_rate(self) -> None:
        self._respond(200, {"failure_rate": _read_failure_rate()})

    def _handle_set_rate(self) -> None:
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length > 0 else b"{}"
        try:
            body = json.loads(raw)
        except json.JSONDecodeError:
            self._respond(400, {"error": "invalid JSON"})
            return

        rate = body.get("failure_rate")
        if rate is None or not isinstance(rate, int) or rate < 0 or rate > 100:
            self._respond(400, {"error": "failure_rate must be an integer 0-100"})
            return

        _write_failure_rate(rate)
        self._respond(200, {"failure_rate": rate})

    def _respond(self, code: int, payload: dict) -> None:
        body = json.dumps(payload, indent=2).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main() -> None:
    # Initialize failure rate file with default
    _write_failure_rate(_read_failure_rate())
    server = HTTPServer(("0.0.0.0", 9002), ControlPanelHandler)
    print("Control Panel listening on :9002")
    server.serve_forever()


if __name__ == "__main__":
    main()
