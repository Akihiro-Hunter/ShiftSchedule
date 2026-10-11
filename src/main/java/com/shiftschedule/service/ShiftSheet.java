package com.shiftschedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;

/** 朝アシシフト1期間分の表（1ユーザーにつき上段・下段の2段） */
public record ShiftSheet(Period period, List<LocalDate> dates, List<Row> rows, boolean confirmed,
		LocalDateTime confirmedAt) {

	public static final String DAY_OFF_MARK = "公";
	public static final String REQUESTED_OFF_MARK = "希";

	/**
	 * 1ユーザー分の行。
	 *
	 * @param requestNote 次回勤務表作成の備考
	 * @param requestUpdatedAt 次回勤務表作成を最後に保存した日時（未保存なら null）
	 */
	public record Row(Long accountId, String userNumber, String name, List<Cell> cells, String requestNote,
			LocalDateTime requestUpdatedAt) {
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
