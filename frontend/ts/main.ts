// =========================================================
// 朝アシシフト 画面スクリプト（エントリーポイント）
// 各機能は data-* 属性が付いた要素があるときだけ動く
// =========================================================
import { initConfirm } from './modules/confirm';
import { initDirtyGuard } from './modules/dirty-guard';
import { initShiftLock } from './modules/shift-lock';
import { initSortable } from './modules/sortable';
import { initWorkerTotal } from './modules/worker-total';

function init(): void {
	initConfirm();
	initDirtyGuard();
	initSortable();
	initWorkerTotal();
	initShiftLock();
}

if (document.readyState === 'loading') {
	document.addEventListener('DOMContentLoaded', init);
} else {
	init();
}
