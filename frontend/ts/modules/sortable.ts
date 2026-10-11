/**
 * ドラッグ＆ドロップで並び替える（ユーザー勤務表管理のユーザーの並び）。
 * 並び順はフォーム内の hidden（name="order"）の順番で送信される。
 * つまみにフォーカスして ↑ ↓ キーでも動かせる。
 *
 *   <table data-sortable>
 *     <tbody data-sortable-item>
 *       … <span data-sortable-handle draggable="true" tabindex="0">⠿</span>
 *       … <input type="hidden" name="order" value="1">
 *     </tbody>
 *   </table>
 */
const ITEM_SELECTOR = '[data-sortable-item]';
const HANDLE_SELECTOR = '[data-sortable-handle]';
const DRAGGING_CLASS = 'is-dragging';
const MOVED_CLASS = 'is-moved';

export function initSortable(): void {
	document.querySelectorAll<HTMLElement>('[data-sortable]').forEach(setup);
}

function setup(container: HTMLElement): void {
	const items = (): HTMLElement[] => Array.from(container.querySelectorAll<HTMLElement>(ITEM_SELECTOR));
	const initial = items();
	let dragging: HTMLElement | null = null;

	/** 元の位置から動いたものに印を付ける */
	const markMoved = (): void => {
		items().forEach((item, i) => item.classList.toggle(MOVED_CLASS, initial[i] !== item));
	};

	container.addEventListener('dragstart', (event) => {
		const handle = (event.target as Element).closest(HANDLE_SELECTOR);
		const item = handle?.closest<HTMLElement>(ITEM_SELECTOR);
		if (!item || !event.dataTransfer) return;
		dragging = item;
		event.dataTransfer.effectAllowed = 'move';
		// Firefox は何かデータを入れないとドラッグが始まらない
		event.dataTransfer.setData('text/plain', '');
		const rect = item.getBoundingClientRect();
		event.dataTransfer.setDragImage(item, event.clientX - rect.left, event.clientY - rect.top);
		// ドラッグ画像を作った後に半透明にする
		requestAnimationFrame(() => {
			if (dragging === item) item.classList.add(DRAGGING_CLASS);
		});
	});

	container.addEventListener('dragover', (event) => {
		if (!dragging) return;
		event.preventDefault();
		const target = (event.target as Element).closest<HTMLElement>(ITEM_SELECTOR);
		if (!target || target === dragging || !container.contains(target)) return;
		const rect = target.getBoundingClientRect();
		const before = event.clientY < rect.top + rect.height / 2;
		target.parentNode?.insertBefore(dragging, before ? target : target.nextSibling);
	});

	container.addEventListener('drop', (event) => {
		if (dragging) event.preventDefault();
	});

	container.addEventListener('dragend', () => {
		dragging?.classList.remove(DRAGGING_CLASS);
		dragging = null;
		markMoved();
	});

	container.addEventListener('keydown', (event) => {
		if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
		const handle = (event.target as Element).closest<HTMLElement>(HANDLE_SELECTOR);
		const item = handle?.closest<HTMLElement>(ITEM_SELECTOR);
		if (!handle || !item) return;
		event.preventDefault();
		const list = items();
		const index = list.indexOf(item);
		if (event.key === 'ArrowUp' && index > 0) {
			item.parentNode?.insertBefore(item, list[index - 1]);
		} else if (event.key === 'ArrowDown' && index < list.length - 1) {
			item.parentNode?.insertBefore(item, list[index + 1].nextSibling);
		}
		handle.focus();
		markMoved();
	});
}
