// Command webhookreceiver is a small HTTP server used by the Standalone
// Activities Instruqt track to receive and inspect webhook deliveries.
//
// It mirrors the behavior of the TypeScript reference implementation
// (typescript/course-repo/server/webhookReceiver.ts) but is written using
// only the Go standard library so the sandbox image does not need a Node
// runtime.
package main

import (
	"encoding/json"
	"io"
	"log"
	"net/http"
	"strconv"
	"sync"
	"time"
)

// Delivery records a single accepted webhook delivery.
type Delivery struct {
	ReceivedAt     string  `json:"receivedAt"`
	IdempotencyKey *string `json:"idempotencyKey"`
	Body           any     `json:"body"`
}

type deliveryJSON struct {
	ReceivedAt     string  `json:"received_at"`
	IdempotencyKey *string `json:"idempotency_key"`
	Body           any     `json:"body"`
}

// state holds all mutable server state, guarded by mu.
var (
	mu               sync.Mutex
	receivedCount    int
	processedCount   int
	dedupedCount     int
	throttledCount   int
	rateLimit        int
	windowTimestamps []time.Time
	seenKeys         = map[string]bool{}
	deliveries       []Delivery
)

func resetState() {
	receivedCount = 0
	processedCount = 0
	dedupedCount = 0
	throttledCount = 0
	rateLimit = 0
	windowTimestamps = nil
	seenKeys = map[string]bool{}
	deliveries = nil
}

// isRateLimited mirrors the TS isRateLimited(): a sliding 1-second window.
// Must be called with mu held.
func isRateLimited() bool {
	if rateLimit == 0 {
		return false
	}
	now := time.Now()
	cutoff := now.Add(-1 * time.Second)

	pruned := make([]time.Time, 0, len(windowTimestamps))
	for _, t := range windowTimestamps {
		if t.After(cutoff) {
			pruned = append(pruned, t)
		}
	}
	windowTimestamps = pruned

	if len(windowTimestamps) >= rateLimit {
		return true
	}
	windowTimestamps = append(windowTimestamps, now)
	return false
}

func statsJSON() map[string]any {
	ds := make([]deliveryJSON, 0, len(deliveries))
	for _, d := range deliveries {
		ds = append(ds, deliveryJSON{
			ReceivedAt:     d.ReceivedAt,
			IdempotencyKey: d.IdempotencyKey,
			Body:           d.Body,
		})
	}
	return map[string]any{
		"received_count":  receivedCount,
		"processed_count": processedCount,
		"deduped_count":   dedupedCount,
		"throttled_count": throttledCount,
		"rate_limit":      rateLimit,
		"count":           processedCount,
		"deliveries":      ds,
	}
}

const htmlPage = `<!DOCTYPE html>
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
</body></html>`

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	enc := json.NewEncoder(w)
	enc.SetIndent("", "  ")
	_ = enc.Encode(v)
}

func handleHooks(w http.ResponseWriter, r *http.Request) {
	mu.Lock()
	defer mu.Unlock()

	receivedCount++

	if isRateLimited() {
		throttledCount++
		writeJSON(w, http.StatusTooManyRequests, map[string]any{"error": "Too Many Requests"})
		return
	}

	var body any = map[string]any{}
	if raw, err := io.ReadAll(r.Body); err == nil {
		var parsed any
		if err := json.Unmarshal(raw, &parsed); err == nil {
			body = parsed
		}
	}

	var idempotencyKey *string
	if key := r.Header.Get("Idempotency-Key"); key != "" {
		idempotencyKey = &key
	}

	if idempotencyKey != nil && seenKeys[*idempotencyKey] {
		dedupedCount++
		writeJSON(w, http.StatusOK, map[string]any{"deduped": true, "idempotencyKey": *idempotencyKey})
		return
	}

	if idempotencyKey != nil {
		seenKeys[*idempotencyKey] = true
	}

	processedCount++
	deliveries = append(deliveries, Delivery{
		ReceivedAt:     time.Now().UTC().Format("2006-01-02T15:04:05.000Z"),
		IdempotencyKey: idempotencyKey,
		Body:           body,
	})

	writeJSON(w, http.StatusOK, map[string]any{"ok": true, "processed": processedCount})
}

func handleReset(w http.ResponseWriter, r *http.Request) {
	mu.Lock()
	resetState()
	mu.Unlock()
	writeJSON(w, http.StatusOK, map[string]any{"reset": true})
}

func handleRateLimit(w http.ResponseWriter, r *http.Request) {
	limit, err := strconv.Atoi(r.URL.Query().Get("limit"))
	if err != nil {
		limit = 0
	}

	mu.Lock()
	rateLimit = limit
	windowTimestamps = nil
	mu.Unlock()

	writeJSON(w, http.StatusOK, map[string]any{"rateLimit": limit})
}

func handleReceived(w http.ResponseWriter, r *http.Request) {
	mu.Lock()
	stats := statsJSON()
	mu.Unlock()
	writeJSON(w, http.StatusOK, stats)
}

func handleRoot(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/html")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(htmlPage))
}

func router(w http.ResponseWriter, r *http.Request) {
	switch {
	case r.Method == http.MethodPost && r.URL.Path == "/hooks":
		handleHooks(w, r)
	case r.Method == http.MethodPost && r.URL.Path == "/_reset":
		handleReset(w, r)
	case r.Method == http.MethodPost && r.URL.Path == "/_rate_limit":
		handleRateLimit(w, r)
	case r.URL.Path == "/_received":
		handleReceived(w, r)
	case r.Method == http.MethodGet && (r.URL.Path == "/" || r.URL.Path == ""):
		handleRoot(w, r)
	default:
		http.NotFound(w, r)
	}
}

func main() {
	http.HandleFunc("/", router)

	const port = ":9000"
	log.Println("Webhook receiver listening on " + port)
	if err := http.ListenAndServe("0.0.0.0"+port, nil); err != nil {
		log.Fatal(err)
	}
}
