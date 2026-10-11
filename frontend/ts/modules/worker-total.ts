/**
 * ユーザー勤務表管理：「公」の切り替えに合わせて合計を数え直す。
 *   - 日ごとの出勤人数（表の一番下）
 *   - ユーザーごとの出勤回数・公年休数（15日と備考の間）
 * 上段のマス（data-day）のうち、「公」にチェックが入っている・休み希望で固定の日を休みとみなす。
 *
 *   <form data-worker-total>
 *     <tbody data-sortable-item>
 *       <td data-day="2026-10-16"> <input type="checkbox"> または <span class="mark-locked"> </td>
 *       <td data-work-days>23</td> <td data-off-days>8</td>
 *     </tbody>
 *     <td data-worker-count="2026-10-16">3</td>
 *   </form>
 */
export function initWorkerTotal(): void {
	const form = document.querySelector<HTMLFormElement>('form[data-worker-total]');
	if (!form) return;

	const totals = Array.from(form.querySelectorAll<HTMLElement>('[data-worker-count]'));
	const users = Array.from(form.querySelectorAll<HTMLElement>('[data-sortable-item]'));

	const isOff = (cell: Element): boolean =>
		cell.querySelector('.mark-locked') !== null ||
		(cell.querySelector<HTMLInputElement>('input[type="checkbox"]')?.checked ?? false);

	const update = (): void => {
		for (const total of totals) {
			const cells = form.querySelectorAll(`[data-day="${total.dataset.workerCount}"]`);
			total.textContent = String(Array.from(cells).filter((cell) => !isOff(cell)).length);
		}
		for (const user of users) {
			const cells = Array.from(user.querySelectorAll('[data-day]'));
			const off = cells.filter(isOff).length;
			const work = user.querySelector('[data-work-days]');
			const offDays = user.querySelector('[data-off-days]');
			if (work) work.textContent = String(cells.length - off);
			if (offDays) offDays.textContent = String(off);
		}
	};

	form.addEventListener('change', (event) => {
		if (event.target instanceof HTMLInputElement && event.target.type === 'checkbox') update();
	});
}
