/**
 * ユーザー勤務表管理：上段の「公」を右クリックすると「年」（年休）に切り替える。もう一度右クリックで「公」に戻る。
 * 休みでないマスを右クリックしたときは、そのマスを「年」にする。
 *
 *   <label data-off-toggle>
 *     <input type="checkbox"> <span>公</span> <input type="hidden" data-leave-input value="false">
 *   </label>
 */
export function initPaidLeave(): void {
	const form = document.querySelector<HTMLFormElement>('form[data-worker-total]');
	if (!form) return;

	const render = (toggle: HTMLElement): void => {
		const leave = toggle.querySelector<HTMLInputElement>('[data-leave-input]');
		const label = toggle.querySelector<HTMLElement>('span');
		if (label) label.textContent = leave?.value === 'true' ? '年' : '公';
	};

	form.addEventListener('contextmenu', (event) => {
		const toggle = (event.target as Element).closest<HTMLElement>('[data-off-toggle]');
		if (!toggle || toggle.closest('.is-locked')) return;
		event.preventDefault();
		const check = toggle.querySelector<HTMLInputElement>('input[type="checkbox"]');
		const leave = toggle.querySelector<HTMLInputElement>('[data-leave-input]');
		if (!check || !leave) return;
		const becomePaid = !check.checked || leave.value !== 'true';
		leave.value = becomePaid ? 'true' : 'false';
		check.checked = true;
		render(toggle);
		// 合計の数え直しと未保存の検知は change イベントに乗せる
		check.dispatchEvent(new Event('change', { bubbles: true }));
	});

	// 休みを外したら「公」に戻す（次に休みにしたときに「年」が残らないように）
	form.addEventListener('change', (event) => {
		const target = event.target;
		if (!(target instanceof HTMLInputElement) || target.type !== 'checkbox' || target.checked) return;
		const toggle = target.closest<HTMLElement>('[data-off-toggle]');
		const leave = toggle?.querySelector<HTMLInputElement>('[data-leave-input]');
		if (toggle && leave) {
			leave.value = 'false';
			render(toggle);
		}
	});
}
