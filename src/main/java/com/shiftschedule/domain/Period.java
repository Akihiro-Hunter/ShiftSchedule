package com.shiftschedule.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 勤務表の期間（毎月16日開始、翌月15日締め）。
 *
 * @param start 期間の開始日（必ず16日）
 */
public record Period(LocalDate start) {

	public static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

	private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("yyyy年M月d日");

	public Period {
		if (start.getDayOfMonth() != 16) {
			throw new IllegalArgumentException("期間の開始日は16日である必要があります: " + start);
		}
	}

	/** 指定日を含む期間 */
	public static Period containing(LocalDate date) {
		LocalDate start = date.getDayOfMonth() >= 16
				? date.withDayOfMonth(16)
				: date.minusMonths(1).withDayOfMonth(16);
		return new Period(start);
	}

	/** 今期 */
	public static Period current() {
		return containing(LocalDate.now(ZONE));
	}

	/** 来期 */
	public static Period upcoming() {
		return current().next();
	}

	/** 開始日の文字列（yyyy-MM-dd）から期間を作る。不正な値なら来期を返す */
	public static Period parseOrUpcoming(String start) {
		if (start == null || start.isBlank()) {
			return upcoming();
		}
		try {
			return new Period(LocalDate.parse(start));
		} catch (RuntimeException e) {
			return upcoming();
		}
	}

	public LocalDate end() {
		return start.plusMonths(1).withDayOfMonth(15);
	}

	public Period next() {
		return new Period(start.plusMonths(1));
	}

	public Period previous() {
		return new Period(start.minusMonths(1));
	}

	public boolean contains(LocalDate date) {
		return !date.isBefore(start) && !date.isAfter(end());
	}

	public List<LocalDate> dates() {
		return start.datesUntil(end().plusDays(1)).toList();
	}

	public String label() {
		return LABEL.format(start) + "〜" + LABEL.format(end());
	}
}
