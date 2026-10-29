package com.shiftschedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.shiftschedule.domain.Half;
import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;

/**
 * 朝アシシフト1期間分の表（1ユーザーにつき上段・下段の2段）。
 *
 * @param plannedCounts 日ごとの予定人数（dates と同じ順）
 */
public record ShiftSheet(Period period, List<LocalDate> dates, List<Row> rows, List<Integer> plannedCounts,
		HalfStatus first, HalfStatus second) {

	/** 半月の確定状態 */
	public record HalfStatus(boolean confirmed, LocalDateTime confirmedAt) {
	}

	public static final String DAY_OFF_MARK = "公";
	public static final String PAID_LEAVE_MARK = "年";
	public static final String REQUESTED_OFF_MARK = "希";

	public HalfStatus status(Half half) {
		return half == Half.FIRST ? first : second;
	}

	/** どちらかの半月が確定している（ユーザーに公開する内容がある） */
	public boolean anyConfirmed() {
		return first.confirmed() || second.confirmed();
	}

	/** 確定済みの半月だけに絞った表（ユーザーに見せる用） */
	public ShiftSheet onlyConfirmed() {
		List<Integer> keep = new ArrayList<>();
		for (int i = 0; i < dates.size(); i++) {
			if (status(period.halfOf(dates.get(i))).confirmed()) {
				keep.add(i);
			}
		}
		List<Row> keptRows = rows.stream().map(row -> row.only(keep)).toList();
		return new ShiftSheet(period, keep.stream().map(dates::get).toList(), keptRows,
				plannedCounts.isEmpty() ? plannedCounts : keep.stream().map(plannedCounts::get).toList(), first, second);
	}

	/** 指定した半月の中に、7連勤以上の日がある人の氏名 */
	public List<String> overLimitNames(Half half) {
		List<String> names = new ArrayList<>();
		for (Row row : rows) {
			for (int i = 0; i < dates.size(); i++) {
				if (period.halfOf(dates.get(i)) == half && row.isOverLimit(i)) {
					names.add(row.name());
					break;
				}
			}
		}
		return names;
	}

	/** 日ごとの出勤人数（上段が「公」でない人の数。dates と同じ順） */
	public List<Integer> workerCounts() {
		List<Integer> counts = new ArrayList<>();
		for (int i = 0; i < dates.size(); i++) {
			int count = 0;
			for (Row row : rows) {
				if (!row.cells().get(i).dayOff()) {
					count++;
				}
			}
			counts.add(count);
		}
		return counts;
	}

	/**
	 * 1ユーザー分の行。
	 *
	 * @param tasksLabel 可能作業（例：クール、ネコポス。なければ空文字）
	 * @param requestNote 次回勤務表作成の備考
	 * @param requestUpdatedAt 次回勤務表作成を最後に保存した日時（未保存なら null）
	 * @param editLocked 管理者が鍵をかけた（この人の勤怠は変更しない）
	 * @param contractDays 契約勤務日数（週あたり）
	 * @param carryIn 表の先頭の日の直前から続いている出勤日数（前の期にまたがる連勤の判定用）
	 */
	public record Row(Long accountId, String userNumber, String name, String tasksLabel, List<Cell> cells,
			String requestNote, LocalDateTime requestUpdatedAt, boolean editLocked, int carryIn, String startTime,
			String endTime, int contractDays) {

		/** 勤務時間は朝アシの既定（5:00〜8:00） */
		public Row(Long accountId, String userNumber, String name, String tasksLabel, List<Cell> cells,
				String requestNote, LocalDateTime requestUpdatedAt, boolean editLocked, int carryIn) {
			this(accountId, userNumber, name, tasksLabel, cells, requestNote, requestUpdatedAt, editLocked, carryIn,
					"5:00", "8:00", 5);
		}

		/** 各日が属する連勤の長さ（休みの日は 0）。半月の区切りに関係なく通して数える */
		public List<Integer> runLengths() {
			return ConsecutiveWork.runLengths(carryIn, cells.stream().map(cell -> !cell.dayOff()).toList());
		}

		/** 期間内で一番長い連勤 */
		public int maxRun() {
			return runLengths().stream().mapToInt(Integer::intValue).max().orElse(0);
		}

		/** i 番目の日が 7連勤以上の中にあるか */
		public boolean isOverLimit(int i) {
			return runLengths().get(i) > ConsecutiveWork.MAX_DAYS;
		}

		/** 指定した位置の日だけに絞った行（ユーザー向け。連勤の判定には使わない） */
		Row only(List<Integer> indexes) {
			return new Row(accountId, userNumber, name, tasksLabel, indexes.stream().map(cells::get).toList(),
					requestNote, requestUpdatedAt, editLocked, 0, startTime, endTime, contractDays);
		}

		/** 出勤回数（上段が「公」でない日数） */
		public int workDays() {
			return (int) cells.stream().filter(cell -> !cell.dayOff()).count();
		}

		/** 公年休数（上段が「公」の日数。休み希望の日も含む） */
		public int dayOffDays() {
			return (int) cells.stream().filter(Cell::dayOff).count();
		}

		/**
		 * 日曜始まりのカレンダー表示用に、1週7マスずつに区切る。
		 * 期間の前後で埋まらないマスは null。
		 */
		public List<List<Cell>> calendarWeeks() {
			List<Cell> slots = new ArrayList<>();
			if (!cells.isEmpty()) {
				// getValue() は月曜=1〜日曜=7。日曜を0にした数だけ先頭を空ける
				slots.addAll(Collections.nCopies(cells.getFirst().date().getDayOfWeek().getValue() % 7, null));
			}
			slots.addAll(cells);
			while (slots.size() % 7 != 0) {
				slots.add(null);
			}
			List<List<Cell>> weeks = new ArrayList<>();
			for (int i = 0; i < slots.size(); i += 7) {
				weeks.add(slots.subList(i, i + 7));
			}
			return weeks;
		}
	}

	/**
	 * 1日分のマス。
	 *
	 * @param request ユーザーの希望（未入力なら null）
	 * @param dayOff 上段が「公」または「年」か
	 * @param bottom 下段の文字
	 * @param locked ユーザーが休みを希望した日（管理者は変更不可）
	 * @param paidLeave 休みが年休（「年」）か
	 */
	public record Cell(LocalDate date, RequestStatus request, boolean dayOff, String bottom, boolean locked,
			boolean paidLeave) {

		public Cell(LocalDate date, RequestStatus request, boolean dayOff, String bottom, boolean locked) {
			this(date, request, dayOff, bottom, locked, false);
		}

		public String top() {
			return !dayOff ? "" : paidLeave ? PAID_LEAVE_MARK : DAY_OFF_MARK;
		}

		/** 画面のフォーム項目名に使うキー */
		public String key(Long accountId) {
			return accountId + "_" + date;
		}
	}
}
