import http from 'http';
import fs from 'fs';
import path from 'path';

const DEMOS_DIR = process.env.DEMOS_DIR ?? '/opt/workshop/demos';
const PORT = 9001;

const MIME: Record<string, string> = {
  '.html': 'text/html; charset=utf-8',
  '.js':   'application/javascript',
  '.css':  'text/css',
  '.png':  'image/png',
  '.svg':  'image/svg+xml',
};

const server = http.createServer((req, res) => {
  const url = req.url ?? '/';
  const rel = url === '/' ? 'heartbeat-demo/index.html' : url.replace(/^\//, '');
  const filePath = path.join(DEMOS_DIR, rel);

  if (!fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not found');
    return;
  }

  const ext = path.extname(filePath);
  res.writeHead(200, {
    'Content-Type': MIME[ext] ?? 'text/plain',
    'X-Frame-Options': 'ALLOWALL',
    'Access-Control-Allow-Origin': '*',
  });
  fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`Demo server listening on :${PORT}`);
});
