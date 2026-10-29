// 管理者画面の左ナビゲーションの開閉。状態はブラウザに覚えておく
const STORAGE_KEY = 'managerSideCollapsed';
const COLLAPSED_CLASS = 'side-collapsed';
const READY_CLASS = 'side-ready';

// 閉じた状態は <head> のスクリプト（fragments/layout.html）が描画前に <html> へ付けている
const root = document.documentElement;

export function initSideNav(): void {
	const toggle = document.querySelector<HTMLButtonElement>('[data-side-toggle]');
	if (!toggle) return;

	const apply = (collapsed: boolean): void => {
		root.classList.toggle(COLLAPSED_CLASS, collapsed);
		toggle.setAttribute('aria-expanded', String(!collapsed));
	};

	apply(read() === '1');
	requestAnimationFrame(() => root.classList.add(READY_CLASS));
	toggle.addEventListener('click', () => {
		const collapsed = !root.classList.contains(COLLAPSED_CLASS);
		apply(collapsed);
		write(collapsed ? '1' : '0');
	});
}

// localStorage が使えない環境でも画面は動かす
function read(): string | null {
	try {
		return localStorage.getItem(STORAGE_KEY);
	} catch {
		return null;
	}
}

function write(value: string): void {
	try {
		localStorage.setItem(STORAGE_KEY, value);
	} catch {
		/* 保存できなくても開閉は有効 */
	}
}
