/**
 * 未保存の変更があるままページを離れようとしたら警告する。
 * data-dirty-guard の付いたフォームが対象。
 */
export function initDirtyGuard(): void {
	const forms = Array.from(document.querySelectorAll<HTMLFormElement>('form[data-dirty-guard]'));
	if (forms.length === 0) return;

	const initial = new Map(forms.map((form) => [form, snapshot(form)]));
	let submitting = false;

	document.addEventListener('submit', (event) => {
		if (!event.defaultPrevented) submitting = true;
	});

	window.addEventListener('beforeunload', (event) => {
		if (submitting) return;
		const dirty = forms.some((form) => snapshot(form) !== initial.get(form));
		if (dirty) {
			event.preventDefault();
			// 古いブラウザ向け
			event.returnValue = '';
		}
	});
}

/** フォームの入力内容を比較用の文字列にする */
function snapshot(form: HTMLFormElement): string {
	const pairs: string[] = [];
	new FormData(form).forEach((value, name) => {
		if (typeof value === 'string') pairs.push(`${name}=${value}`);
	});
	return pairs.join('&');
}
