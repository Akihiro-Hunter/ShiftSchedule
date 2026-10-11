// =========================================================
// 開発用：frontend/ を監視して自動ビルドし、プレビューを配信する。
// ビルドのたびにブラウザが自動で再読み込みされる。
//   npm run dev  →  http://localhost:5173
// =========================================================
import { createServer } from 'node:http';
import { watch } from 'node:fs';
import { readFile, stat } from 'node:fs/promises';
import { extname, join, normalize } from 'node:path';
import { buildAll, SRC, PREVIEW } from './build.mjs';

const PORT = Number(process.env.PORT ?? 5173);

const TYPES = {
	'.html': 'text/html; charset=utf-8',
	'.css': 'text/css; charset=utf-8',
	'.js': 'text/javascript; charset=utf-8',
	'.svg': 'image/svg+xml',
	'.png': 'image/png',
};

// iframe の中ではなく、一番外側のページだけが再読み込みする
const RELOAD_SCRIPT = `<script>
if (window.top === window) new EventSource('/__reload').onmessage = () => location.reload();
</script>`;

const clients = new Set();

const server = createServer(async (req, res) => {
	const path = decodeURIComponent(new URL(req.url, 'http://x').pathname);

	if (path === '/__reload') {
		res.writeHead(200, {
			'Content-Type': 'text/event-stream',
			'Cache-Control': 'no-cache',
			Connection: 'keep-alive',
		});
		res.write('\n');
		clients.add(res);
		req.on('close', () => clients.delete(res));
		return;
	}

	let file = normalize(join(PREVIEW, path));
	if (!file.startsWith(PREVIEW)) {
		res.writeHead(403).end();
		return;
	}
	try {
		if ((await stat(file)).isDirectory()) file = join(file, 'index.html');
		let body = await readFile(file);
		const type = TYPES[extname(file)] ?? 'application/octet-stream';
		if (type.startsWith('text/html')) {
			body = body.toString().replace('</body>', `${RELOAD_SCRIPT}</body>`);
		}
		res.writeHead(200, { 'Content-Type': type, 'Cache-Control': 'no-store' });
		res.end(body);
	} catch {
		res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' }).end('Not Found');
	}
});

let building = false;
let queued = false;

async function rebuild(reason) {
	if (building) {
		queued = true;
		return;
	}
	building = true;
	try {
		const ms = await buildAll();
		console.log(`✔ ${new Date().toLocaleTimeString()} ビルド完了 (${ms}ms)${reason ? ' ← ' + reason : ''}`);
		for (const client of clients) client.write('data: reload\n\n');
	} catch (e) {
		console.error('✖ ビルド失敗\n' + (e.formatted ?? e.message ?? e));
	} finally {
		building = false;
		if (queued) {
			queued = false;
			rebuild();
		}
	}
}

let timer;
watch(SRC, { recursive: true }, (_event, filename) => {
	clearTimeout(timer);
	timer = setTimeout(() => rebuild(filename), 80);
});

await rebuild();
server.listen(PORT, () => {
	console.log(`\n  見た目確認 → http://localhost:${PORT}/`);
	console.log('  frontend/ 以下を保存すると自動でビルド＆再読み込みします（Ctrl+C で終了）\n');
});
