package com.shiftschedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.shiftschedule.service.ShiftGenerator.Assignment;
import com.shiftschedule.service.ShiftGenerator.Problem;
import com.shiftschedule.service.ShiftGenerator.Result;
import com.shiftschedule.service.ShiftGenerator.StaffInfo;
import com.shiftschedule.service.ShiftGenerator.TaskNeed;

class ShiftGeneratorTest {

	private static final LocalDate START = LocalDate.of(2026, 11, 16); // 月曜
	private static final long DRY = 1, LIFT = 2, PICK = 3;
	private static final long SORT_GROUP = 10, TRUCK_GROUP = 11;

	private static List<LocalDate> days(int n) {
		return START.datesUntil(START.plusDays(n)).toList();
	}

	private static Problem problem(List<LocalDate> dates, List<StaffInfo> staff, List<TaskNeed> needs,
			Map<Long, Map<LocalDate, Boolean>> fixed, int planned) {
		Map<Long, Set<LocalDate>> free = new HashMap<>();
		Map<Long, Map<LocalDate, Boolean>> fixedWorking = new HashMap<>();
		for (StaffInfo s : staff) {
			Set<LocalDate> f = new HashSet<>(dates);
			Map<LocalDate, Boolean> fw = new HashMap<>(fixed.getOrDefault(s.id(), Map.of()));
			f.removeAll(fw.keySet());
			free.put(s.id(), f);
			fixedWorking.put(s.id(), fw);
		}
		Map<LocalDate, Integer> plan = new HashMap<>();
		dates.forEach(d -> plan.put(d, planned));
		return new Problem(dates, staff, needs, free, fixedWorking, List.of(), plan);
	}

	private static List<StaffInfo> staff(int count, int contract, Set<Long> tasks) {
		List<StaffInfo> list = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			list.add(new StaffInfo(i, contract, tasks));
		}
		return list;
	}

	private static long workingDays(Result r, long staffId, List<LocalDate> week) {
		return week.stream().filter(d -> r.working().get(staffId).getOrDefault(d, false)).count();
	}

	@Test
	void 週の契約日数を超えず_必要人数を満たす() {
		List<LocalDate> dates = days(14);
		List<StaffInfo> staff = staff(8, 5, Set.of(DRY, LIFT));
		List<TaskNeed> needs = List.of(new TaskNeed(DRY, SORT_GROUP, true, 3), new TaskNeed(LIFT, TRUCK_GROUP, true, 1));
		Result r = ShiftGenerator.solve(problem(dates, staff, needs, Map.of(), 5));

		for (StaffInfo s : staff) {
			assertTrue(workingDays(r, s.id(), dates.subList(0, 7)) <= 5);
			assertTrue(workingDays(r, s.id(), dates.subList(7, 14)) <= 5);
		}
		for (LocalDate d : dates) {
			assertEquals(3, r.assignments().stream().filter(a -> a.date().equals(d) && a.taskId() == DRY).count());
			assertEquals(1, r.assignments().stream().filter(a -> a.date().equals(d) && a.taskId() == LIFT).count());
		}
	}

	@Test
	void 同じ時間帯の作業は1人1日1つまで() {
		List<LocalDate> dates = days(7);
		List<StaffInfo> staff = staff(6, 5, Set.of(DRY, PICK));
		// DRY と PICK は同じ大項目。人数が足りなくても、1人が両方に入ってはいけない
		List<TaskNeed> needs = List.of(new TaskNeed(DRY, SORT_GROUP, true, 4), new TaskNeed(PICK, SORT_GROUP, true, 4));
		Result r = ShiftGenerator.solve(problem(dates, staff, needs, Map.of(), 6));

		Map<String, Long> perPersonDay = new HashMap<>();
		for (Assignment a : r.assignments()) {
			perPersonDay.merge(a.staffId() + "/" + a.date(), 1L, Long::sum);
		}
		assertTrue(perPersonDay.values().stream().allMatch(c -> c <= 1));
	}

	@Test
	void 作業ができない人は入らず_休み希望の日は出勤しない() {
		List<LocalDate> dates = days(7);
		StaffInfo canLift = new StaffInfo(1, 6, Set.of(LIFT));
		StaffInfo cannot = new StaffInfo(2, 6, Set.of(DRY));
		Map<Long, Map<LocalDate, Boolean>> fixed = Map.of(1L, Map.of(dates.get(2), false));
		List<TaskNeed> needs = List.of(new TaskNeed(LIFT, SORT_GROUP, true, 1));
		Result r = ShiftGenerator.solve(problem(dates, List.of(canLift, cannot), needs, fixed, 2));

		assertTrue(r.assignments().stream().allMatch(a -> a.staffId() == 1));
		assertTrue(r.working().get(1L).get(dates.get(2)) == null);
	}

	@Test
	void 人が足りない日は契約日数を守って不足のままにする() {
		List<LocalDate> dates = days(7);
		List<StaffInfo> staff = staff(2, 3, Set.of(DRY));
		List<TaskNeed> needs = List.of(new TaskNeed(DRY, SORT_GROUP, true, 2));
		Result r = ShiftGenerator.solve(problem(dates, staff, needs, Map.of(), 2));

		for (StaffInfo s : staff) {
			assertEquals(3, workingDays(r, s.id(), dates));
		}
	}

	@Test
	void 人手が足りない日は数日に集中させず均等に分ける() {
		List<LocalDate> dates = days(7);
		// 6人×週3日=18人日。予定は毎日5人（計35人日）で、どうしても足りない
		List<StaffInfo> staff = staff(6, 3, Set.of(DRY));
		Result r = ShiftGenerator.solve(problem(dates, staff, List.of(), Map.of(), 5));

		List<Long> perDay = dates.stream()
				.map(d -> staff.stream().filter(s -> r.working().get(s.id()).get(d)).count()).toList();
		long max = perDay.stream().mapToLong(Long::longValue).max().orElse(0);
		long min = perDay.stream().mapToLong(Long::longValue).min().orElse(0);
		assertTrue(max - min <= 1, "日ごとの出勤人数 " + perDay);
	}

	@Test
	void 十日連続で出勤しない() {
		List<LocalDate> dates = days(14);
		List<StaffInfo> staff = staff(4, 6, Set.of(DRY));
		List<TaskNeed> needs = List.of(new TaskNeed(DRY, SORT_GROUP, true, 3));
		Result r = ShiftGenerator.solve(problem(dates, staff, needs, Map.of(), 3));

		for (StaffInfo s : staff) {
			int run = 0;
			int max = 0;
			for (LocalDate d : dates) {
				run = r.working().get(s.id()).get(d) ? run + 1 : 0;
				max = Math.max(max, run);
			}
			assertTrue(max <= 6, "連勤 " + max);
		}
		assertEquals(DayOfWeek.MONDAY, START.getDayOfWeek());
	}
}
