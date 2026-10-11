package com.shiftschedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.shiftschedule.domain.Period;

class ShiftSheetTest {

	private static ShiftSheet.Row row(Period period) {
		List<ShiftSheet.Cell> cells = period.dates().stream()
				.map(d -> new ShiftSheet.Cell(d, null, false, "", false))
				.toList();
		return new ShiftSheet.Row(1L, "10000001", "テスト", cells, null, null);
	}

	@Test
	void カレンダーは日曜始まりで前後を空ける() {
		// 2026/9/16 は水曜、2026/10/15 は木曜
		List<List<ShiftSheet.Cell>> weeks = row(new Period(LocalDate.of(2026, 9, 16))).calendarWeeks();

		assertEquals(5, weeks.size());
		weeks.forEach(week -> assertEquals(7, week.size()));
		assertNull(weeks.get(0).get(2));
		assertEquals(LocalDate.of(2026, 9, 16), weeks.get(0).get(3).date());
		assertEquals(LocalDate.of(2026, 10, 15), weeks.get(4).get(4).date());
		assertNull(weeks.get(4).get(5));
	}

	@Test
	void 日曜開始なら先頭は空けない() {
		// 2025/11/16 は日曜
		List<List<ShiftSheet.Cell>> weeks = row(new Period(LocalDate.of(2025, 11, 16))).calendarWeeks();

		assertEquals(LocalDate.of(2025, 11, 16), weeks.get(0).get(0).date());
	}
}
