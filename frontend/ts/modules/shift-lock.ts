/**
 * ユーザー勤務表管理：鍵を押したユーザーの勤怠を、この画面で変更できなくする。
 * 鍵の状態は hidden（data-lock-input）に入れて、保存ボタンで送る。
 * 鍵をかけた行は tbody に is-locked が付き、名前と番号が赤くなる（CSS）。
 *
 *   <tbody data-sortable-item class="is-locked">
 *     <button type="button" data-lock-toggle aria-pressed="true">鍵</button>
 *     <input type="hidden" name="lock_1" value="true" data-lock-input>
 *     <label class="off-toggle"><input type="checkbox"></label>
 *     <input type="text" class="note-input" readonly>
 *   </tbody>
 */
const ITEM_SELECTOR = '[data-sortable-item]';
const LOCKED_CLASS = 'is-locked';

export function initShiftLock(): void {
	const form = document.querySelector<HTMLFormElement>('form[data-worker-total]');
	if (!form) return;

	form.addEventListener('click', (event) => {
		const target = event.target as Element;

		const toggle = target.closest<HTMLButtonElement>('[data-lock-toggle]');
		if (toggle) {
			const item = toggle.closest<HTMLElement>(ITEM_SELECTOR);
			if (item) setLocked(item, toggle.getAttribute('aria-pressed') !== 'true');
			return;
		}

		// 鍵がかかった行の「公」は切り替えさせない（ラベルのクリックもここに来る）
		if (target instanceof HTMLInputElement && target.type === 'checkbox' &&
				target.closest(`${ITEM_SELECTOR}.${LOCKED_CLASS}`)) {
			event.preventDefault();
		}
	});
}

function setLocked(item: HTMLElement, locked: boolean): void {
	item.classList.toggle(LOCKED_CLASS, locked);

	const toggle = item.querySelector<HTMLButtonElement>('[data-lock-toggle]');
	if (toggle) {
		toggle.setAttribute('aria-pressed', String(locked));
		toggle.title = locked ? 'ロック中（押すと解除）' : 'ロックする（押すと勤怠を変更不可に）';
	}
	const input = item.querySelector<HTMLInputElement>('[data-lock-input]');
	if (input) input.value = String(locked);

	item.querySelectorAll<HTMLInputElement>('.note-input').forEach((note) => {
		note.readOnly = locked;
	});
}
