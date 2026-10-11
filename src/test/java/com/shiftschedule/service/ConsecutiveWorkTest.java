package com.shiftschedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConsecutiveWorkTest {

	private static final boolean W = true;
	private static final boolean O = false;

	@Test
	void 休みを挟むと連勤は途切れる() {
		assertEquals(List.of(2, 2, 0, 3, 3, 3), ConsecutiveWork.runLengths(0, List.of(W, W, O, W, W, W)));
	}

	@Test
	void 前の期から続く出勤を先頭の連勤に足す() {
		assertEquals(List.of(7, 7, 0, 1), ConsecutiveWork.runLengths(5, List.of(W, W, O, W)));
	}

	@Test
	void 先頭が休みなら前の期の出勤は数えない() {
		assertEquals(List.of(0, 1), ConsecutiveWork.runLengths(6, List.of(O, W)));
	}

	@Test
	void 前半と後半をまたぐ連勤も通しで数える() {
		// 前半の末尾4日 + 後半の先頭3日 = 7連勤
		List<Boolean> days = List.of(O, W, W, W, W, W, W, W, O);
		List<Integer> lengths = ConsecutiveWork.runLengths(0, days);
		assertEquals(7, lengths.get(4));
		assertEquals(true, lengths.get(4) > ConsecutiveWork.MAX_DAYS);
	}
}
