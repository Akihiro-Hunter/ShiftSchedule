// =========================================================
// 見た目確認用のプレビュー画面（サンプルデータ入りの静的 HTML）
//
// Spring・DB を起動しなくても、ビルドした CSS / JS の見た目を確認するためのもの。
// マークアップは src/main/resources/templates/ の Thymeleaf テンプレートと
// 同じ構造にしてある。テンプレートの HTML を変えたら、ここも合わせて直すこと。
// =========================================================

// ---------- サンプルデータ ----------

const CURRENT = { start: new Date(2026, 8, 16), label: '2026年9月16日〜2026年10月15日' };
const NEXT = { start: new Date(2026, 9, 16), label: '2026年10月16日〜2026年11月15日' };

const WEEK = ['日', '月', '火', '水', '木', '金', '土'];
const TASKS = ['クール', 'ネコポス', '苗穂', '環状', '光星', 'リフト'];
const STATUSES = [
	{ name: 'AVAILABLE', label: '出勤可能' },
	{ name: 'OFF', label: '休み' },
	{ name: 'UNDECIDED', label: '未定' },
];
const NOTE_MAX_LENGTH = 10;
const LOGIN_USER = '佐藤 花子';

const ACCOUNTS = [
	{ id: 1, no: '10000001', name: '山田 太郎', role: 'MANAGER', birth: '1980/04/12', hire: '2010/04/01', leave: 0, tasks: [] },
	{ id: 2, no: '20240001', name: '佐藤 花子', role: 'USER', birth: '2001/07/03', hire: '2022/10/16', leave: 10, tasks: ['クール', 'ネコポス', 'リフト'] },
	{ id: 3, no: '20240002', name: '鈴木 一郎', role: 'USER', birth: '1998/12/24', hire: '2023/04/01', leave: 7.5, tasks: ['苗穂', '環状'] },
	{ id: 4, no: '20240003', name: '高橋 美咲', role: 'PART', birth: '2003/02/14', hire: '2024/06/16', leave: 3, tasks: ['クール', '光星'] },
	{ id: 5, no: '20240004', name: '田中 健太', role: 'USER', birth: '1995/09/30', hire: '2021/01/16', leave: 12.5, tasks: TASKS },
	{ id: 6, no: '20240005', name: '伊藤 さくら', role: 'USER', birth: '2004/05/05', hire: '2025/03/16', leave: 0, tasks: ['ネコポス'] },
];
const ROLES = { MANAGER: '管理者', USER: 'アルバイト', PART: 'パート' };
const USERS = ACCOUNTS.filter((a) => a.role !== 'MANAGER');

/** 毎回同じ結果になる疑似乱数（見た目がビルドごとに変わらないように） */
function random(seed) {
	let x = seed;
	return () => {
		x = (x * 1103515245 + 12345) % 2147483648;
		return x / 2147483648;
	};
}

function periodDates(period) {
	const dates = [];
	const d = new Date(period.start);
	const end = new Date(d.getFullYear(), d.getMonth() + 1, 15);
	while (d <= end) {
		dates.push(new Date(d));
		d.setDate(d.getDate() + 1);
	}
	return dates;
}

const week = (d) => WEEK[d.getDay()];
const weekClass = (d) => (d.getDay() === 6 ? 'sat' : d.getDay() === 0 ? 'sun' : '');
const monthDayWithWeek = (d) => `${d.getMonth() + 1}/${d.getDate()}(${week(d)})`;
const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

/** 朝アシシフト表のサンプル（ShiftSheet 相当） */
function buildSheet(period, { withRequests }) {
	const dates = periodDates(period);
	const rows = USERS.map((u) => {
		const rnd = random(u.id * 97);
		const cells = dates.map((date) => {
			const r = rnd();
			const request = !withRequests || u.id === 6 ? null : r < 0.22 ? 'OFF' : r < 0.32 ? 'UNDECIDED' : 'AVAILABLE';
			const locked = request === 'OFF';
			const dayOff = locked || (request !== 'OFF' && rnd() < 0.12);
			const bottom = locked ? '希' : !dayOff && rnd() < 0.25 ? TASKS[Math.floor(rnd() * TASKS.length)] : '';
			return { date, request, locked, dayOff, top: dayOff ? '公' : '', bottom };
		});
		return {
			...u,
			cells,
			note: u.id === 3 ? '10/25 は午前のみ出勤できます。\n11月は試験のため少なめ希望' : u.id === 2 ? '特になし' : '',
			updatedAt: u.id === 6 ? null : '10/8 21:14',
		};
	});
	return { dates, rows };
}

/** 日曜始まりのカレンダー用に7マスずつ区切る（ShiftSheet.Row#calendarWeeks 相当） */
function calendarWeeks(cells) {
	const slots = [...Array(cells[0].date.getDay()).fill(null), ...cells];
	while (slots.length % 7 !== 0) slots.push(null);
	const weeks = [];
	for (let i = 0; i < slots.length; i += 7) weeks.push(slots.slice(i, i + 7));
	return weeks;
}

// ---------- 共通パーツ（fragments/layout.html 相当） ----------

const esc = (v) =>
	String(v ?? '').replace(/[&<>"']/g, (ch) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[ch]);

/** 実際の URL → プレビューのファイル */
const LINKS = {
	'/manager/login': 'login.html',
	'/manager/menu': 'manager-menu.html',
	'/manager/users': 'manager-users.html',
	'/manager/users/new': 'manager-user-form.html',
	'/manager/shifts': 'manager-shifts.html',
	'/user/menu': 'user-menu.html',
	'/user/shift': 'user-shift.html',
	'/user/request': 'user-request.html',
	'/user/tasks': 'user-tasks.html',
	'/user/profile': 'user-profile.html',
};
const href = (url) => LINKS[url] ?? '#';

function page({ title, bodyClass, body }) {
	return `<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${esc(title)} | 朝アシシフト（プレビュー）</title>
<link rel="stylesheet" href="../css/app.css">
<script src="../js/app.js" defer></script>
<script>
// プレビューにはサーバーが無いので、フォームは送信しない
document.addEventListener('submit', (e) => e.preventDefault());
</script>
</head>
<body class="${bodyClass}">
${body}
</body>
</html>
`;
}

const header = (title, area) => `
	<header class="app-header">
		<h1 class="app-title">${esc(title)}</h1>
		<div class="app-user">
			<span class="app-user-name">${esc(area === 'manager' ? '山田 太郎' : LOGIN_USER)}</span>
			<form action="${href(`/${area}/login`)}" method="post">
				<button type="submit" class="btn btn-small">ログアウト</button>
			</form>
		</div>
	</header>`;

const messages = ({ message, error } = {}) => `
		<div>
			${message ? `<p class="flash flash-ok">${esc(message)}</p>` : ''}
			${error ? `<p class="flash flash-error">${esc(error)}</p>` : ''}
		</div>`;

const back = (area) => `<a class="back-link" href="${href(`/${area}/menu`)}">← メニューへ</a>`;

// ---------- 各画面 ----------

function login() {
	return page({
		title: 'ログイン',
		bodyClass: 'login-page',
		body: `
	<main class="login-box">
		<h1 class="login-title">朝アシシフト</h1>
		<p class="flash flash-error">ユーザー番号またはパスワードが正しくありません</p>
		<form action="${href('/user/menu')}" method="post" class="form">
			<label class="form-row">
				<span class="form-label">ユーザー番号</span>
				<input type="text" name="userNumber" inputmode="numeric" maxlength="8" pattern="\\d{8}"
					title="半角数字8桁" autocomplete="username" required>
			</label>
			<label class="form-row">
				<span class="form-label">パスワード</span>
				<input type="password" name="password" pattern="[A-Za-z0-9]{8,}"
					title="半角英数字8文字以上" autocomplete="current-password" required>
			</label>
			<button type="submit" class="btn btn-primary btn-block">ログイン</button>
		</form>
	</main>`,
	});
}

function menu(area, items) {
	return page({
		title: 'メニュー',
		bodyClass: area,
		body: `${header('メニュー', area)}
	<main class="container">
		<nav class="menu">
			${items
				.map(
					([url, text, desc], i) => `
			<a class="menu-item" href="${href(url)}">
				<span class="menu-no">${i + 1}</span>
				<span class="menu-text">${text}</span>
				<span class="menu-desc">${desc}</span>
			</a>`,
				)
				.join('')}
		</nav>
	</main>`,
	});
}

function managerUsers() {
	const rows = ACCOUNTS.map(
		(a) => `
					<tr>
						<td>${a.no}</td>
						<td>${esc(a.name)}</td>
						<td><span class="badge${a.role === 'MANAGER' ? ' badge-manager' : a.role === 'PART' ? ' badge-part' : ''}">${ROLES[a.role]}</span></td>
						<td>${a.birth}</td>
						<td>${a.hire}</td>
						<td class="num">${a.leave}日</td>
						<td>${a.tasks.join('、')}</td>
						<td class="actions">
							<a class="btn btn-small" href="${href('/manager/users/new')}">編集</a>
							<form method="post" data-confirm="${esc(a.name)} さんを削除します。勤務表のデータも削除されます。よろしいですか？">
								<button type="submit" class="btn btn-small btn-danger">削除</button>
							</form>
						</td>
					</tr>`,
	).join('');
	return page({
		title: 'ユーザー情報管理',
		bodyClass: 'manager',
		body: `${header('ユーザー情報管理', 'manager')}
	<main class="container">
		<div class="toolbar">
			${back('manager')}
			<a class="btn btn-primary" href="${href('/manager/users/new')}">＋ 新規作成</a>
		</div>
		${messages({ message: '佐藤 花子 さんの情報を保存しました' })}
		<div class="table-wrap">
			<table class="table">
				<thead>
					<tr>
						<th>ユーザー番号</th>
						<th>氏名</th>
						<th>役職</th>
						<th>生年月日</th>
						<th>入社日</th>
						<th class="num">残有給</th>
						<th>可能作業</th>
						<th></th>
					</tr>
				</thead>
				<tbody>${rows}
				</tbody>
			</table>
		</div>
	</main>`,
	});
}

function managerUserForm() {
	const a = ACCOUNTS[1];
	return page({
		title: 'ユーザー編集',
		bodyClass: 'manager',
		body: `${header('ユーザー編集', 'manager')}
	<main class="container container-narrow">
		<div class="toolbar">
			<a class="back-link" href="${href('/manager/users')}">← ユーザー一覧へ</a>
		</div>

		<form method="post" class="form card" data-dirty-guard>
			<label class="form-row">
				<span class="form-label">ユーザー番号 <em class="req">必須</em></span>
				<input type="text" name="userNumber" value="${a.no}" inputmode="numeric" maxlength="8" required>
				<span class="hint">半角数字8桁</span>
			</label>

			<label class="form-row">
				<span class="form-label">氏名 <em class="req">必須</em></span>
				<input type="text" name="name" value="${esc(a.name)}" maxlength="50" required>
				<span class="field-error">氏名を入力してください（エラー表示の例）</span>
			</label>

			<label class="form-row">
				<span class="form-label">役職</span>
				<select name="role">
					<option value="MANAGER">管理者</option>
					<option value="USER" selected>アルバイト</option>
					<option value="PART">パート</option>
				</select>
			</label>

			<div class="form-cols">
				<label class="form-row">
					<span class="form-label">生年月日</span>
					<input type="date" name="birthDate" value="${a.birth.replaceAll('/', '-')}">
				</label>
				<label class="form-row">
					<span class="form-label">入社日</span>
					<input type="date" name="hireDate" value="${a.hire.replaceAll('/', '-')}">
				</label>
			</div>
			<p class="hint">アルバイト・パートの場合、生年月日と入社日は必須です</p>

			<label class="form-row">
				<span class="form-label">残年次有給休暇数（日）</span>
				<input type="number" name="paidLeaveDays" value="${a.leave}" min="0" max="99.5" step="0.5" required class="input-short">
			</label>

			<fieldset class="form-row">
				<legend class="form-label">可能作業</legend>
				<div class="check-group">
					${TASKS.map(
						(t) => `
					<label class="check">
						<input type="checkbox" name="tasks" value="${t}"${a.tasks.includes(t) ? ' checked' : ''}>
						<span>${t}</span>
					</label>`,
					).join('')}
				</div>
			</fieldset>

			<label class="form-row">
				<span class="form-label">パスワード再設定</span>
				<input type="password" name="password" autocomplete="new-password">
				<span class="hint">変更する場合のみ入力（半角英数字8文字以上）</span>
			</label>

			<div class="form-actions">
				<button type="submit" class="btn btn-primary">保存</button>
			</div>
		</form>
	</main>`,
	});
}

function managerShifts() {
	const sheet = buildSheet(NEXT, { withRequests: true });
	const reqClass = (cell) => `req-${cell.request ? cell.request.toLowerCase() : 'none'}`;
	const reqTitle = (cell) => STATUSES.find((s) => s.name === cell.request)?.label ?? '未入力';

	const body = sheet.rows
		.map(
			(row) => `
					<tbody class="sheet-user" data-sortable-item>
						<tr class="sheet-top">
							<th class="sheet-name" rowspan="2">
								<div class="sheet-name-inner">
									<span class="drag-handle" draggable="true" data-sortable-handle tabindex="0"
										title="ドラッグ（または ↑↓ キー）で並び替え" aria-label="並び替え">⠿</span>
									<div>
										<span>${esc(row.name)}</span><br>
										<small>${row.no}</small>
									</div>
								</div>
								<input type="hidden" name="order" value="${row.id}">
							</th>
							${row.cells
								.map(
									(cell) => `<td class="${reqClass(cell)}" title="${reqTitle(cell)}">${
										cell.locked
											? '<span class="mark mark-locked">公</span>'
											: `<label class="off-toggle"><input type="checkbox" name="off_${row.id}_${iso(cell.date)}"${cell.dayOff ? ' checked' : ''}><span>公</span></label>`
									}</td>`,
								)
								.join('')}
							<td class="sheet-remark" rowspan="2">
								<div class="remark-text">${esc(row.note)}</div>
								${row.updatedAt ? `<small class="muted">更新 ${row.updatedAt}</small>` : '<small class="muted">未提出</small>'}
							</td>
						</tr>
						<tr class="sheet-bottom">
							${row.cells
								.map(
									(cell) => `<td class="${reqClass(cell)}">${
										cell.locked
											? '<span class="mark mark-locked">希</span>'
											: `<input type="text" class="note-input" name="note_${row.id}_${iso(cell.date)}" value="${esc(cell.bottom)}" maxlength="${NOTE_MAX_LENGTH}">`
									}</td>`,
								)
								.join('')}
						</tr>
					</tbody>`,
		)
		.join('');

	return page({
		title: 'ユーザー勤務表管理',
		bodyClass: 'manager',
		body: `${header('ユーザー勤務表管理', 'manager')}
	<main class="container container-wide">
		<div class="toolbar">
			${back('manager')}
			<div class="period-nav">
				<a class="btn btn-small" href="#">← 前期</a>
				<span class="period-label">
					<span>${NEXT.label}</span>
					<span class="badge">来期</span>
				</span>
				<a class="btn btn-small" href="#">次期 →</a>
			</div>
			<a class="btn" href="#">CSV出力</a>
		</div>
		${messages({ message: '途中保存しました' })}

		<div class="shift-status">
			<span class="badge badge-warn">未確定（ユーザーには非公開）</span>
			<span class="legend">
				ユーザーの希望：
				<span class="legend-item req-available">出勤可能</span>
				<span class="legend-item req-undecided">未定</span>
				<span class="legend-item req-off">休み（変更不可）</span>
				<span class="legend-item req-none">未入力</span>
			</span>
		</div>
		<p class="hint">上段のマスをクリックすると「公」（休み）と空白（出勤）が切り替わります。下段には自由に入力できます（<span>${NOTE_MAX_LENGTH}</span>文字まで）。<br>
			氏名の左の <span class="drag-handle-sample">⠿</span> をドラッグ（または ↑↓ キー）すると並び順を変えられます（保存で反映され、ユーザーの勤務表確認・CSV・PDFも同じ順になります）。</p>

		<form method="post" id="shift-form" data-dirty-guard>
			<div class="sheet-wrap">
				<table class="sheet" data-sortable>
					<thead>
						<tr>
							<th class="sheet-name">氏名</th>
							${sheet.dates.map((d) => `<th class="sheet-date ${weekClass(d)}"><span>${d.getDate()}</span><br><small>${week(d)}</small></th>`).join('')}
							<th class="sheet-remark">備考（ユーザー入力）</th>
						</tr>
					</thead>${body}
				</table>
			</div>

			<div class="form-actions">
				<button type="submit" name="action" value="save" class="btn">途中保存</button>
				<button type="submit" name="action" value="confirm" class="btn btn-primary"
					data-confirm="保存して確定します。確定するとユーザーの勤務表確認に表示されます。よろしいですか？">保存して確定</button>
			</div>
		</form>
	</main>`,
	});
}

function userShift() {
	const sheet = buildSheet(CURRENT, { withRequests: true });
	const me = sheet.rows.find((r) => r.name === LOGIN_USER);
	const meClass = (row) => (row === me ? ' is-me' : '');

	return page({
		title: '勤務表確認',
		bodyClass: 'user',
		body: `${header('勤務表確認', 'user')}
	<main class="container">
		${back('user')}

		<div class="tabs">
			<a class="tab tab-active" href="#">今期</a>
			<a class="tab" href="#">来期</a>
		</div>

		<h2 class="section-title">
			<span>今期</span>
			<small>${CURRENT.label}</small>
		</h2>

		<section class="card">
			<h3 class="card-title">あなたの勤務</h3>
			<table class="my-calendar">
				<thead>
					<tr>${WEEK.map((w, i) => `<th${i === 0 ? ' class="sun"' : i === 6 ? ' class="sat"' : ''}>${w}</th>`).join('')}</tr>
				</thead>
				<tbody>
					${calendarWeeks(me.cells)
						.map(
							(w) => `
					<tr>
						${w
							.map((cell) =>
								cell
									? `<td><span class="cal-date ${weekClass(cell.date)}">${cell.date.getDate() === 1 || cell.date.getTime() === CURRENT.start.getTime() ? `${cell.date.getMonth() + 1}/${cell.date.getDate()}` : cell.date.getDate()}</span><span class="cal-mark${cell.dayOff ? ' is-off' : ''}">${cell.dayOff ? '休' : '出'}</span>${cell.locked ? '' : `<span class="cal-note">${esc(cell.bottom)}</span>`}</td>`
									: '<td class="is-blank"></td>',
							)
							.join('')}
					</tr>`,
						)
						.join('')}
				</tbody>
			</table>
		</section>

		<section class="card">
			<h3 class="card-title">朝アシシフト（全員）</h3>
			<p class="hint">横にスクロールできます</p>
			<div class="sheet-wrap">
				<table class="sheet sheet-readonly">
					<thead>
						<tr>
							<th class="sheet-name">氏名</th>
							${sheet.dates.map((d) => `<th class="sheet-date ${weekClass(d)}"><span>${d.getDate()}</span><br><small>${week(d)}</small></th>`).join('')}
						</tr>
					</thead>
					<tbody>
						${sheet.rows
							.map(
								(row) => `
						<tr class="sheet-top${meClass(row)}">
							<th class="sheet-name" rowspan="2">${esc(row.name)}</th>
							${row.cells.map((c) => `<td>${c.top}</td>`).join('')}
						</tr>
						<tr class="sheet-bottom${meClass(row)}">
							${row.cells.map((c) => `<td>${esc(c.bottom)}</td>`).join('')}
						</tr>`,
							)
							.join('')}
					</tbody>
				</table>
			</div>
		</section>
		<div class="download-actions">
			<a class="btn btn-block" href="#">朝アシシフトをPDFでダウンロード（A4横）</a>
		</div>
	</main>`,
	});
}

function userRequest() {
	const dates = periodDates(NEXT);
	const rnd = random(7);
	// まだ選んでいない日は「出勤可能」（休み・未定だけ変える）
	const statuses = new Map(
		dates.map((d) => {
			const r = rnd();
			return [iso(d), r < 0.75 ? 'AVAILABLE' : r < 0.92 ? 'OFF' : 'UNDECIDED'];
		}),
	);

	return page({
		title: '次回勤務表作成',
		bodyClass: 'user',
		body: `${header('次回勤務表作成', 'user')}
	<main class="container">
		${back('user')}
		${messages({ message: '保存しました' })}

		<h2 class="section-title">
			来期 <small>${NEXT.label}</small>
		</h2>
		<p class="hint">
			はじめはすべての日が「出勤可能」になっています。休みや未定の日だけ変えて保存してください。途中で保存できます。
			<br>
			<span>最終保存：10/8 21:14</span>
		</p>

		<form method="post" data-dirty-guard>
			<div class="req-list">
				${dates
					.map(
						(d) => `
				<div class="req-day">
					<span class="req-date ${weekClass(d)}">${monthDayWithWeek(d)}</span>
					<div class="seg">
						${STATUSES.map(
							(s) => `<label class="seg-${s.name.toLowerCase()}"><input type="radio" name="status_${iso(d)}" value="${s.name}"${statuses.get(iso(d)) === s.name ? ' checked' : ''}><span>${s.label}</span></label>`,
						).join('')}
					</div>
				</div>`,
					)
					.join('')}
			</div>

			<label class="form-row">
				<span class="form-label">備考</span>
				<textarea name="note" rows="4" maxlength="200" placeholder="管理者への連絡事項があれば入力してください">特になし</textarea>
			</label>

			<div class="sticky-actions">
				<button type="submit" class="btn btn-primary btn-block">保存</button>
			</div>
		</form>
	</main>`,
	});
}

function userTasks() {
	const me = ACCOUNTS.find((a) => a.name === LOGIN_USER);
	return page({
		title: '可能作業',
		bodyClass: 'user',
		body: `${header('可能作業', 'user')}
	<main class="container">
		${back('user')}
		${messages()}

		<form method="post" class="card" data-dirty-guard>
			<p class="hint">できる作業にチェックを入れて保存してください。</p>
			<div class="check-list">
				${TASKS.map(
					(t) => `
				<label class="check check-large">
					<input type="checkbox" name="tasks" value="${t}"${me.tasks.includes(t) ? ' checked' : ''}>
					<span>${t}</span>
				</label>`,
				).join('')}
			</div>
			<button type="submit" class="btn btn-primary btn-block">保存</button>
		</form>
	</main>`,
	});
}

function userProfile() {
	const me = ACCOUNTS.find((a) => a.name === LOGIN_USER);
	const jp = (s) => {
		const [y, m, d] = s.split('/').map(Number);
		return `${y}年${m}月${d}日`;
	};
	return page({
		title: '個人情報設定',
		bodyClass: 'user',
		body: `${header('個人情報設定', 'user')}
	<main class="container">
		${back('user')}
		${messages({ error: '現在のパスワードが正しくありません（エラー表示の例）' })}

		<section class="card">
			<h3 class="card-title">個人情報</h3>
			<dl class="info">
				<dt>氏名</dt>
				<dd>${esc(me.name)}</dd>
				<dt>ユーザー番号</dt>
				<dd>${me.no}</dd>
				<dt>生年月日</dt>
				<dd>${jp(me.birth)}</dd>
				<dt>入社日</dt>
				<dd>${jp(me.hire)}</dd>
				<dt>残年次有給休暇数</dt>
				<dd>${me.leave}日</dd>
				<dt>可能作業</dt>
				<dd>${me.tasks.join('、') || '-'}</dd>
			</dl>
			<p class="hint">個人情報の変更は管理者に依頼してください。</p>
		</section>

		<section class="card">
			<h3 class="card-title">パスワード変更</h3>
			<form method="post" class="form">
				<label class="form-row">
					<span class="form-label">現在のパスワード</span>
					<input type="password" name="currentPassword" autocomplete="current-password" required>
					<span class="field-error">現在のパスワードが正しくありません</span>
				</label>
				<label class="form-row">
					<span class="form-label">新しいパスワード</span>
					<input type="password" name="newPassword" autocomplete="new-password" required>
					<span class="hint">半角英数字8文字以上</span>
				</label>
				<label class="form-row">
					<span class="form-label">新しいパスワード（確認）</span>
					<input type="password" name="confirmPassword" autocomplete="new-password" required>
				</label>
				<button type="submit" class="btn btn-primary btn-block">変更する</button>
			</form>
		</section>
	</main>`,
	});
}

// ---------- 画面一覧（index.html） ----------

/** [ファイル名, 画面名, 区分, 主に使う端末] */
const SCREENS = [
	['login.html', 'ログイン', '共通', 'both', login],
	['manager-menu.html', 'メニュー', '管理者', 'pc', () =>
		menu('manager', [
			['/manager/users', 'ユーザー情報管理', 'アルバイト・パートの個人情報の作成・編集'],
			['/manager/shifts', 'ユーザー勤務表管理', '朝アシシフトの作成・確定・CSV出力'],
		])],
	['manager-users.html', '1. ユーザー情報管理', '管理者', 'pc', managerUsers],
	['manager-user-form.html', '1-1. ユーザー編集', '管理者', 'pc', managerUserForm],
	['manager-shifts.html', '2. ユーザー勤務表管理', '管理者', 'pc', managerShifts],
	['user-menu.html', 'メニュー', 'アルバイト', 'sp', () =>
		menu('user', [
			['/user/shift', '勤務表確認', '今期の朝アシシフトを確認'],
			['/user/request', '次回勤務表作成', '来期の出勤可能日と備考を入力'],
			['/user/tasks', '可能作業', 'できる作業をチェック'],
			['/user/profile', '個人情報設定', '個人情報の確認・パスワード変更'],
		])],
	['user-shift.html', '1. 勤務表確認', 'アルバイト', 'sp', userShift],
	['user-request.html', '2. 次回勤務表作成', 'アルバイト', 'sp', userRequest],
	['user-tasks.html', '3. 可能作業', 'アルバイト', 'sp', userTasks],
	['user-profile.html', '4. 個人情報設定', 'アルバイト', 'sp', userProfile],
];

function index() {
	const builtAt = new Date().toLocaleString('ja-JP');
	const cards = SCREENS.map(
		([file, title, group, device]) => `
		<section class="screen" data-device="${device}">
			<header>
				<span class="group">${group}</span>
				<h2>${title}</h2>
				<a href="pages/${file}" target="_blank">単独で開く ↗</a>
			</header>
			<div class="frames">
				<figure class="frame frame-pc">
					<div class="viewport"><iframe src="pages/${file}" loading="lazy" title="${title}（PC）"></iframe></div>
					<figcaption>PC 1280px</figcaption>
				</figure>
				<figure class="frame frame-sp">
					<div class="viewport"><iframe src="pages/${file}" loading="lazy" title="${title}（スマホ）"></iframe></div>
					<figcaption>スマホ 390px</figcaption>
				</figure>
			</div>
		</section>`,
	).join('');

	return `<!DOCTYPE html>
<html lang="ja">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>画面プレビュー | 朝アシシフト</title>
<style>
	:root { color-scheme: light; --line: #d9dee3; --muted: #6b7680; }
	* { box-sizing: border-box; }
	body { margin: 0; background: #e9edf1; color: #1f2933; font-family: 'Hiragino Sans', 'Yu Gothic UI', 'Meiryo', sans-serif; }
	.top { position: sticky; top: 0; z-index: 10; display: flex; flex-wrap: wrap; align-items: center; gap: 12px 20px;
		padding: 12px 20px; background: #24303c; color: #fff; }
	.top h1 { margin: 0; font-size: 18px; }
	.top .built { font-size: 12px; opacity: .7; }
	.top .filter { margin-left: auto; display: flex; gap: 4px; }
	.top button { padding: 6px 12px; border: 1px solid #5a6672; border-radius: 6px; background: transparent; color: #fff; font: inherit; font-size: 13px; cursor: pointer; }
	.top button[aria-pressed="true"] { background: #1f6feb; border-color: #1f6feb; }
	main { padding: 20px; display: grid; gap: 24px; }
	.screen { min-width: 0; overflow-x: auto; background: #fff; border: 1px solid var(--line); border-radius: 10px; padding: 14px 16px 16px; }
	.screen header { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; }
	.screen h2 { margin: 0; font-size: 16px; }
	.screen header a { margin-left: auto; font-size: 13px; color: #1f6feb; }
	.group { font-size: 12px; padding: 1px 8px; border-radius: 999px; background: #e8ecf0; }
	.frames { display: flex; flex-wrap: wrap; gap: 20px; align-items: flex-start; }
	.frame { margin: 0; }
	.frame figcaption { margin-top: 4px; font-size: 12px; color: var(--muted); text-align: center; }
	.viewport { overflow: hidden; border: 1px solid var(--line); border-radius: 6px; background: #f4f6f8; }
	.viewport iframe { border: 0; transform-origin: 0 0; display: block; }
	/* PC：1280×800 を 0.5 倍で表示 */
	.frame-pc .viewport { width: 640px; height: 400px; }
	.frame-pc iframe { width: 1280px; height: 800px; transform: scale(.5); }
	/* スマホ：390×760 を 0.6 倍で表示 */
	.frame-sp .viewport { width: 234px; height: 456px; border-radius: 14px; }
	.frame-sp iframe { width: 390px; height: 760px; transform: scale(.6); }
	body[data-show="pc"] .frame-sp, body[data-show="sp"] .frame-pc { display: none; }
	body[data-show="sp"] .frame-sp .viewport { width: 312px; height: 608px; }
	body[data-show="sp"] .frame-sp iframe { transform: scale(.8); }
	body[data-show="sp"] main { grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); }
	body[data-show="pc"] .frame-pc .viewport { width: 960px; height: 600px; }
	body[data-show="pc"] .frame-pc iframe { transform: scale(.75); }
</style>
</head>
<body data-show="both">
	<div class="top">
		<h1>朝アシシフト 画面プレビュー</h1>
		<span class="built">ビルド：${builtAt}（サンプルデータ）</span>
		<div class="filter" role="group" aria-label="表示する端末">
			<button type="button" data-show="both" aria-pressed="true">両方</button>
			<button type="button" data-show="pc" aria-pressed="false">PC</button>
			<button type="button" data-show="sp" aria-pressed="false">スマホ</button>
		</div>
	</div>
	<main>${cards}
	</main>
<script>
	const buttons = document.querySelectorAll('.filter button');
	const apply = (mode) => {
		document.body.dataset.show = mode;
		buttons.forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.show === mode)));
		try { localStorage.setItem('preview-show', mode); } catch {}
	};
	buttons.forEach((b) => b.addEventListener('click', () => apply(b.dataset.show)));
	try { apply(localStorage.getItem('preview-show') || 'both'); } catch {}
</script>
</body>
</html>
`;
}

/** { 出力ファイル名: HTML } */
export function renderAll() {
	const files = { 'index.html': index() };
	for (const [file, , , , render] of SCREENS) files[`pages/${file}`] = render();
	return files;
}
