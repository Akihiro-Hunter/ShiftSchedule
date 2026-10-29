// 操作説明（<details data-hint-fold="キー">）の開閉。慣れたら畳めるよう、状態はブラウザに覚えておく
export function initHintFold(): void {
	document.querySelectorAll<HTMLDetailsElement>('details[data-hint-fold]').forEach((details) => {
		const key = `hintFold:${details.dataset.hintFold}`;
		try {
			if (localStorage.getItem(key) === '0') details.open = false;
		} catch {
			/* 読めなくても開いたまま表示する */
		}
		details.addEventListener('toggle', () => {
			try {
				localStorage.setItem(key, details.open ? '1' : '0');
			} catch {
				/* 保存できなくても開閉は有効 */
			}
		});
	});
}
