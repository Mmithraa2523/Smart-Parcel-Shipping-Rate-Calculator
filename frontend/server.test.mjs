import assert from 'node:assert/strict';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { test } from 'node:test';
import { createServer } from './server.mjs';
import { createServer as createHttpServer } from 'node:http';

test('serves the Angular application and preserves API paths and methods through the backend proxy', async () => {
  const directory = await mkdtemp(join(process.env.TEMP ?? process.cwd(), 'swiftship-server-test-'));
  const angularHtml = '<!doctype html><title>SwiftShip test app</title>';
  await writeFile(join(directory, 'index.html'), angularHtml, 'utf8');

  const backend = createHttpServer((request, response) => {
    let body = '';
    request.setEncoding('utf8');
    request.on('data', chunk => body += chunk);
    request.on('end', () => {
      response.writeHead(201, { 'Content-Type': 'application/json', 'X-Backend': 'swiftship-test' });
      response.end(JSON.stringify({ method: request.method, url: request.url, body }));
    });
  });
  await new Promise(resolve => backend.listen(0, '127.0.0.1', resolve));
  const backendAddress = backend.address();
  assert.ok(backendAddress && typeof backendAddress !== 'string');
  const frontend = createServer({
    frontendDirectory: directory,
    backendUrl: `http://127.0.0.1:${backendAddress.port}`,
  });
  await new Promise(resolve => frontend.listen(0, '127.0.0.1', resolve));
  const frontendAddress = frontend.address();
  assert.ok(frontendAddress && typeof frontendAddress !== 'string');

  try {
    const appResponse = await fetch(`http://127.0.0.1:${frontendAddress.port}/`);
    assert.equal(appResponse.status, 200);
    assert.match(appResponse.headers.get('content-type') ?? '', /text\/html/);
    assert.equal(await appResponse.text(), angularHtml);

    const missingRoute = await fetch(`http://127.0.0.1:${frontendAddress.port}/bookings/reference`);
    assert.equal(await missingRoute.text(), angularHtml);

    const quoteResponse = await fetch(`http://127.0.0.1:${frontendAddress.port}/api/quotes?preview=true`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ weightKg: 1, distanceKm: 10, tier: 'STANDARD' }),
    });
    assert.equal(quoteResponse.status, 201);
    assert.equal(quoteResponse.headers.get('x-backend'), 'swiftship-test');
    assert.deepEqual(await quoteResponse.json(), {
      method: 'POST',
      url: '/api/quotes?preview=true',
      body: JSON.stringify({ weightKg: 1, distanceKm: 10, tier: 'STANDARD' }),
    });

    const unknownApi = await fetch(`http://127.0.0.1:${frontendAddress.port}/api/not-a-route`);
    assert.deepEqual(await unknownApi.json(), { method: 'GET', url: '/api/not-a-route', body: '' });
  } finally {
    await Promise.all([
      new Promise(resolve => frontend.close(resolve)),
      new Promise(resolve => backend.close(resolve)),
    ]);
    await rm(directory, { recursive: true, force: true });
  }
});
