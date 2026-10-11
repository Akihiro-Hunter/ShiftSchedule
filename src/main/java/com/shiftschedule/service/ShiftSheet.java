package com.shiftschedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;

/**
 * 朝アシシフト1期間分の表（1ユーザーにつき上段・下段の2段）。
 *
 * @param plannedCounts 日ごとの予定人数（dates と同じ順）
 */
public record ShiftSheet(Period period, List<LocalDate> dates, List<Row> rows, List<Integer> plannedCounts,
		boolean confirmed, LocalDateTime confirmedAt) {

	public static final String DAY_OFF_MARK = "公";
	public static final String REQUESTED_OFF_MARK = "希";

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
	 */
	public record Row(Long accountId, String userNumber, String name, String tasksLabel, List<Cell> cells,
			String requestNote, LocalDateTime requestUpdatedAt, boolean editLocked) {

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
	 * @param dayOff 上段が「公」か
	 * @param bottom 下段の文字
	 * @param locked ユーザーが休みを希望した日（管理者は変更不可）
	 */
	public record Cell(LocalDate date, RequestStatus request, boolean dayOff, String bottom, boolean locked) {

		public String top() {
			return dayOff ? DAY_OFF_MARK : "";
		}

		/** 画面のフォーム項目名に使うキー */
		public String key(Long accountId) {
			return accountId + "_" + date;
		}
	}
}
