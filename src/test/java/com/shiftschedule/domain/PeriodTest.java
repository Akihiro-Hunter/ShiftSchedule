package com.shiftschedule.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PeriodTest {

	@Test
	void 十五日までは前月16日から始まる期間() {
		Period p = Period.containing(LocalDate.of(2026, 10, 15));
		assertEquals(LocalDate.of(2026, 9, 16), p.start());
		assertEquals(LocalDate.of(2026, 10, 15), p.end());
	}

	@Test
	void 十六日以降は当月16日から始まる期間() {
		Period p = Period.containing(LocalDate.of(2026, 10, 16));
		assertEquals(LocalDate.of(2026, 10, 16), p.start());
		assertEquals(LocalDate.of(2026, 11, 15), p.end());
	}

	@Test
	void 年をまたぐ期間() {
		Period p = Period.containing(LocalDate.of(2027, 1, 3));
		assertEquals(LocalDate.of(2026, 12, 16), p.start());
		assertEquals(LocalDate.of(2027, 1, 15), p.end());
		assertEquals(31, p.dates().size());
		assertEquals(LocalDate.of(2027, 1, 16), p.next().start());
	}

	@Test
	void 二月を含む期間の日数() {
		Period p = new Period(LocalDate.of(2027, 1, 16));
		assertEquals(LocalDate.of(2027, 2, 15), p.end());
		assertEquals(31, p.dates().size());
		assertEquals(28, new Period(LocalDate.of(2027, 2, 16)).dates().size());
	}

	@Test
	void 開始日が16日以外ならエラー() {
		assertThrows(IllegalArgumentException.class, () -> new Period(LocalDate.of(2026, 10, 1)));
	}

	@Test
	void 不正な文字列なら来期() {
		assertEquals(Period.upcoming(), Period.parseOrUpcoming("2026-10-01"));
		assertEquals(Period.upcoming(), Period.parseOrUpcoming("abc"));
		assertEquals(new Period(LocalDate.of(2026, 10, 16)), Period.parseOrUpcoming("2026-10-16"));
	}
	@Test
	void 前半は16日から月末_後半は翌月1日から15日() {
		Period p = new Period(LocalDate.of(2026, 10, 16));
		assertEquals(LocalDate.of(2026, 10, 16), p.start(Half.FIRST));
		assertEquals(LocalDate.of(2026, 10, 31), p.end(Half.FIRST));
		assertEquals(LocalDate.of(2026, 11, 1), p.start(Half.SECOND));
		assertEquals(LocalDate.of(2026, 11, 15), p.end(Half.SECOND));
		assertEquals(16, p.dates(Half.FIRST).size());
		assertEquals(15, p.dates(Half.SECOND).size());
		assertEquals(Half.FIRST, p.halfOf(LocalDate.of(2026, 10, 31)));
		assertEquals(Half.SECOND, p.halfOf(LocalDate.of(2026, 11, 1)));
	}

	@Test
	void 二月を含む期間の前半は月末が28日() {
		Period p = new Period(LocalDate.of(2027, 2, 16));
		assertEquals(LocalDate.of(2027, 2, 28), p.end(Half.FIRST));
		assertEquals(LocalDate.of(2027, 3, 1), p.start(Half.SECOND));
	}
}
