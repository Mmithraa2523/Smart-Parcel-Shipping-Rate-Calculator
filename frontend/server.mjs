import { createServer as createHttpServer, request as createHttpRequest } from 'node:http';
import { readFile, stat } from 'node:fs/promises';
import { extname, join, resolve, sep } from 'node:path';
import { fileURLToPath } from 'node:url';

const projectDirectory = fileURLToPath(new URL('.', import.meta.url));
const defaultFrontendDirectory = resolve(projectDirectory, 'dist', 'smart-shipping', 'browser');
const contentTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.html', 'text/html; charset=utf-8'],
  ['.ico', 'image/x-icon'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.json', 'application/json; charset=utf-8'],
  ['.png', 'image/png'],
  ['.svg', 'image/svg+xml'],
  ['.txt', 'text/plain; charset=utf-8'],
  ['.webp', 'image/webp'],
]);

function sendError(response, statusCode, message) {
  response.writeHead(statusCode, { 'Content-Type': 'text/plain; charset=utf-8' });
  response.end(`${message}\n`);
}

export function createServer({
  frontendDirectory = defaultFrontendDirectory,
  backendUrl = 'http://127.0.0.1:8080',
} = {}) {
  const staticRoot = resolve(frontendDirectory);
  const backend = new URL(backendUrl);

  return createHttpServer(async (request, response) => {
    if (request.url === undefined) {
      sendError(response, 400, 'A request path is required.');
      return;
    }

    let pathname;
    try {
      pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    } catch {
      sendError(response, 400, 'The request path is invalid.');
      return;
    }

    if (pathname === '/api' || pathname.startsWith('/api/')) {
      const upstream = createHttpRequest(new URL(request.url, backend), {
        method: request.method,
        headers: { ...request.headers, host: backend.host },
      }, upstreamResponse => {
        response.writeHead(upstreamResponse.statusCode ?? 502, upstreamResponse.headers);
        upstreamResponse.pipe(response);
      });
      upstream.on('error', () => {
        if (!response.headersSent) {
          sendError(response, 502, 'The shipping API is unavailable.');
        } else {
          response.destroy();
        }
      });
      request.pipe(upstream);
      return;
    }

    if (!['GET', 'HEAD'].includes(request.method ?? 'GET')) {
      sendError(response, 405, 'Only GET and HEAD requests can load application files.');
      return;
    }

    const requestedFile = resolve(staticRoot, `.${pathname}`);
    if (requestedFile !== staticRoot && !requestedFile.startsWith(`${staticRoot}${sep}`)) {
      sendError(response, 404, 'Not found.');
      return;
    }

    try {
      let filePath = requestedFile;
      try {
        if ((await stat(filePath)).isDirectory()) {
          filePath = join(filePath, 'index.html');
        }
      } catch (error) {
        if (error.code !== 'ENOENT') {
          throw error;
        }
        filePath = join(staticRoot, 'index.html');
      }
      const content = await readFile(filePath);
      response.writeHead(200, {
        'Content-Type': contentTypes.get(extname(filePath)) ?? 'application/octet-stream',
        'Cache-Control': extname(filePath) === '.html' ? 'no-cache' : 'public, max-age=3600',
      });
      response.end(request.method === 'HEAD' ? undefined : content);
    } catch (error) {
      if (error.code === 'ENOENT') {
        sendError(response, 503, 'The application has not been built. Run npm run build first.');
      } else {
        console.error('Failed to serve application file.', error);
        sendError(response, 500, 'The application could not load this page.');
      }
    }
  });
}

export function startServer({
  host = process.env.HOST ?? '127.0.0.1',
  port = Number(process.env.PORT ?? 3000),
  backendUrl = process.env.API_BACKEND_URL ?? 'http://127.0.0.1:8080',
  frontendDirectory = defaultFrontendDirectory,
} = {}) {
  const server = createServer({ frontendDirectory, backendUrl });
  server.listen(port, host, () => {
    const address = server.address();
    if (!address || typeof address === 'string') {
      throw new Error('The frontend server did not bind to a TCP port.');
    }
    console.info(`SwiftShip frontend listening on http://${host}:${address.port}`);
    console.info(`Shipping API proxy target: ${backendUrl}`);
  });
  return server;
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  startServer();
}
