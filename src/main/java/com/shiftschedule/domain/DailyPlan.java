package com.shiftschedule.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 日ごとの予定人数（ユーザー勤務表管理で変更したもの。未保存の日は既定値） */
@Entity
@Table(name = "daily_plans")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyPlan {

	public static final int DEFAULT_COUNT = 15;
	public static final int DEFAULT_COUNT_MON_TUE = 13;
	public static final int MAX_COUNT = 999;

	@Id
	@Column(name = "work_date")
	private LocalDate workDate;

	@Column(nullable = false)
	private int plannedCount;

	public DailyPlan(LocalDate workDate, int plannedCount) {
		this.workDate = workDate;
		this.plannedCount = plannedCount;
	}

	/** 既定の予定人数（月・火は13人、それ以外は15人） */
	public static int defaultCount(LocalDate date) {
		DayOfWeek dow = date.getDayOfWeek();
		return dow == DayOfWeek.MONDAY || dow == DayOfWeek.TUESDAY ? DEFAULT_COUNT_MON_TUE : DEFAULT_COUNT;
	}
}
