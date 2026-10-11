/**
 * 次回勤務表作成：未入力の日数をその場で数え直し、未入力の日に印を付ける。
 *
 *   <form data-request-form>
 *     <div class="req-day"> <input type="radio" name="status_2026-10-16"> … </div>
 *   </form>
 *   <span data-unanswered-count>未入力 3日</span>
 */
const DAY_SELECTOR = '.req-day';
const UNANSWERED_CLASS = 'is-unanswered';

export function initRequestCounter(): void {
	const form = document.querySelector<HTMLFormElement>('form[data-request-form]');
	if (!form) return;

	const counter = document.querySelector<HTMLElement>('[data-unanswered-count]');
	const days = Array.from(form.querySelectorAll<HTMLElement>(DAY_SELECTOR));

	const update = (): void => {
		let unanswered = 0;
		for (const day of days) {
			const answered = day.querySelector('input[type="radio"]:checked') !== null;
			day.classList.toggle(UNANSWERED_CLASS, !answered);
			if (!answered) unanswered++;
		}
		if (counter) {
			counter.textContent = `未入力 ${unanswered}日`;
			counter.hidden = unanswered === 0;
		}
	};

	form.addEventListener('change', update);
	update();
}
