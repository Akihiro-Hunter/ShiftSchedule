/**
 * ユーザー勤務表管理：「公」の切り替えに合わせて合計を数え直す。
 *   - 日ごとの出勤人数（表の一番下）
 *   - ユーザーごとの出勤回数・公年休数・最大連勤（15日と備考の間）。7連勤以上の日は赤く表示
 *     （連勤は前半・後半の区切りに関係なく通して数える。期の先頭へ続く日数は tbody の data-carry-in）
 * 上段のマス（data-day）のうち、「公」にチェックが入っている・休み希望で固定の日を休みとみなす。
 *
 *   <form data-worker-total>
 *     <tbody data-sortable-item>
 *       <td data-day="2026-10-16"> <input type="checkbox"> または <span class="mark-locked"> </td>
 *       <td data-work-days>23</td> <td data-off-days>8</td> <td data-max-run>5</td>
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

	const maxRunLimit = Number(form.dataset.maxRun ?? '6');

	// 連勤の更新：各マスが属する連勤の長さを数え、上限を超えた連勤のマスに is-streak を付ける
	const updateStreaks = (user: HTMLElement): void => {
		const cells = Array.from(user.querySelectorAll<HTMLElement>('.sheet-top [data-day]'));
		const lengths: number[] = new Array(cells.length).fill(0);
		let i = 0;
		while (i < cells.length) {
			if (isOff(cells[i])) {
				i++;
				continue;
			}
			let end = i;
			while (end < cells.length && !isOff(cells[end])) end++;
			const length = end - i + (i === 0 ? Number(user.dataset.carryIn ?? '0') : 0);
			for (let j = i; j < end; j++) lengths[j] = length;
			i = end;
		}
		cells.forEach((cell, index) => cell.classList.toggle('is-streak', lengths[index] > maxRunLimit));
		const max = Math.max(0, ...lengths);
		const maxCell = user.querySelector<HTMLElement>('[data-max-run]');
		if (maxCell) {
			maxCell.textContent = String(max);
			maxCell.classList.toggle('is-streak', max > maxRunLimit);
		}
	};

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
			updateStreaks(user);
		}
	};

	form.addEventListener('change', (event) => {
		if (event.target instanceof HTMLInputElement && event.target.type === 'checkbox') update();
	});
}
