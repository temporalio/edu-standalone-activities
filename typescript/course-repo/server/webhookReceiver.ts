import http from 'http';
import { URL } from 'url';

interface Delivery {
  receivedAt: string;
  idempotencyKey: string | null;
  body: unknown;
}

const state = {
  receivedCount: 0,
  processedCount: 0,
  dedupedCount: 0,
  throttledCount: 0,
  rateLimit: 0,
  windowTimestamps: [] as number[],
  seenKeys: new Set<string>(),
  deliveries: [] as Delivery[],
};

function resetState() {
  state.receivedCount = 0;
  state.processedCount = 0;
  state.dedupedCount = 0;
  state.throttledCount = 0;
  state.rateLimit = 0;
  state.windowTimestamps = [];
  state.seenKeys.clear();
  state.deliveries = [];
}

function isRateLimited(): boolean {
  if (state.rateLimit === 0) return false;
  const now = Date.now();
  const cutoff = now - 1000;
  state.windowTimestamps = state.windowTimestamps.filter(t => t > cutoff);
  if (state.windowTimestamps.length >= state.rateLimit) {
    return true;
  }
  state.windowTimestamps.push(now);
  return false;
}

function readBody(req: http.IncomingMessage): Promise<string> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    req.on('data', c => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks).toString()));
    req.on('error', reject);
  });
}

function statsJson(): object {
  return {
    received_count: state.receivedCount,
    processed_count: state.processedCount,
    deduped_count: state.dedupedCount,
    throttled_count: state.throttledCount,
    rate_limit: state.rateLimit,
    count: state.processedCount,
    deliveries: state.deliveries.map(d => ({
      received_at: d.receivedAt,
      idempotency_key: d.idempotencyKey,
      body: d.body,
    })),
  };
}

const HTML_PAGE = `<!DOCTYPE html>
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
</body></html>`;

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url ?? '/', `http://${req.headers.host}`);

  if (req.method === 'POST' && url.pathname === '/hooks') {
    state.receivedCount++;

    if (isRateLimited()) {
      state.throttledCount++;
      res.writeHead(429, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'Too Many Requests' }));
      return;
    }

    let body: unknown = {};
    try {
      const raw = await readBody(req);
      body = JSON.parse(raw);
    } catch { /* malformed body; accept anyway */ }

    const idempotencyKey = req.headers['idempotency-key'] as string | undefined ?? null;

    if (idempotencyKey !== null && state.seenKeys.has(idempotencyKey)) {
      state.dedupedCount++;
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ deduped: true, idempotencyKey }));
      return;
    }

    if (idempotencyKey !== null) {
      state.seenKeys.add(idempotencyKey);
    }

    state.processedCount++;
    state.deliveries.push({
      receivedAt: new Date().toISOString(),
      idempotencyKey,
      body,
    });

    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ ok: true, processed: state.processedCount }));
    return;
  }

  if (req.method === 'POST' && url.pathname === '/_reset') {
    resetState();
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ reset: true }));
    return;
  }

  if (req.method === 'POST' && url.pathname === '/_rate_limit') {
    const limit = parseInt(url.searchParams.get('limit') ?? '0', 10);
    state.rateLimit = isNaN(limit) ? 0 : limit;
    state.windowTimestamps = [];
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ rateLimit: state.rateLimit }));
    return;
  }

  if (url.pathname === '/_received') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(statsJson(), null, 2));
    return;
  }

  if (req.method === 'GET' && (url.pathname === '/' || url.pathname === '')) {
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end(HTML_PAGE);
    return;
  }

  res.writeHead(404);
  res.end('Not found');
});

const PORT = 9000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`Webhook receiver listening on :${PORT}`);
});
