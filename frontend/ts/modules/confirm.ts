/**
 * 送信前の確認ダイアログ。
 * 押されたボタン、またはフォームに data-confirm="メッセージ" があれば confirm() を出す。
 *
 *   <button type="submit" data-confirm="確定します。よろしいですか？">確定</button>
 *   <form data-confirm="削除します。よろしいですか？">…</form>
 */
export function initConfirm(): void {
	document.addEventListener(
		'submit',
		(event) => {
			const form = event.target;
			if (!(form instanceof HTMLFormElement)) return;

			const submitter = event.submitter;
			const message = submitter?.dataset.confirm ?? form.dataset.confirm;
			if (message && !window.confirm(message)) {
				event.preventDefault();
				event.stopImmediatePropagation();
			}
		},
		// 他の submit 処理（未保存チェックなど）より先に判定する
		{ capture: true },
	);
}
