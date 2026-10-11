// =========================================================
// フロントのビルド
//   1. SCSS       → src/main/resources/static/css/app.css
//   2. TypeScript → src/main/resources/static/js/app.js
//   3. 見た目確認用のプレビュー画面 → preview/
// =========================================================
import { mkdir, writeFile, copyFile, rm } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import * as sass from 'sass';
import * as esbuild from 'esbuild';

export const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
export const SRC = join(ROOT, 'frontend');
export const STATIC = join(ROOT, 'src/main/resources/static');
export const PREVIEW = join(ROOT, 'preview');

const CSS_OUT = join(STATIC, 'css/app.css');
const JS_OUT = join(STATIC, 'js/app.js');

async function write(file, content) {
	await mkdir(dirname(file), { recursive: true });
	await writeFile(file, content);
}

export async function buildCss() {
	const { css } = sass.compile(join(SRC, 'scss/app.scss'), { style: 'expanded' });
	await write(CSS_OUT, css + '\n');
}

export async function buildJs() {
	await esbuild.build({
		entryPoints: [join(SRC, 'ts/main.ts')],
		outfile: JS_OUT,
		bundle: true,
		format: 'iife',
		target: 'es2020',
		legalComments: 'none',
		logLevel: 'silent',
	});
}

export async function buildPreview() {
	// npm run dev で変更が反映されるよう、ページ定義は毎回読み直す
	const url = pathToFileURL(join(SRC, 'preview/pages.mjs')).href + `?t=${Date.now()}`;
	const { renderAll } = await import(url);

	await rm(PREVIEW, { recursive: true, force: true });
	for (const [file, html] of Object.entries(renderAll())) {
		await write(join(PREVIEW, file), html);
	}
	await mkdir(join(PREVIEW, 'css'), { recursive: true });
	await mkdir(join(PREVIEW, 'js'), { recursive: true });
	await copyFile(CSS_OUT, join(PREVIEW, 'css/app.css'));
	await copyFile(JS_OUT, join(PREVIEW, 'js/app.js'));
}

export async function buildAll() {
	const started = performance.now();
	await Promise.all([buildCss(), buildJs()]);
	await buildPreview();
	return Math.round(performance.now() - started);
}

// 直接実行されたとき（npm run build）
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
	try {
		const ms = await buildAll();
		console.log(`✔ ビルド完了 (${ms}ms)`);
		console.log(`  CSS : ${CSS_OUT}`);
		console.log(`  JS  : ${JS_OUT}`);
		console.log(`  見た目確認 → ${pathToFileURL(join(PREVIEW, 'index.html')).href}`);
	} catch (e) {
		console.error('✖ ビルド失敗\n' + (e.formatted ?? e.message ?? e));
		process.exit(1);
	}
}
