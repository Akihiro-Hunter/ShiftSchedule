package com.shiftschedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.shiftschedule.domain.Period;

class ShiftCsvWriterTest {

	@Test
	void 紙の勤務表と同じ並びで出力する() {
		Period period = new Period(LocalDate.of(2026, 10, 16));
		List<ShiftSheet.Cell> cells = new ArrayList<>();
		for (LocalDate d : period.dates()) {
			boolean off = d.getDayOfMonth() == 17;
			cells.add(new ShiftSheet.Cell(d, null, off, off ? "希" : d.getDayOfMonth() == 19 ? "クール" : "", off));
		}
		ShiftSheet.Row row = new ShiftSheet.Row(1L, "10000001", "テストユーザー", "クール", cells, "変更もOK\r\n土日休み", null, false, 0);
		List<Integer> planned = period.dates().stream().map(d -> 15).toList();
		ShiftSheet sheet = new ShiftSheet(period, period.dates(), List.of(row), planned, new ShiftSheet.HalfStatus(true, null),
				new ShiftSheet.HalfStatus(true, null));

		String csv = new String(new ShiftCsvWriter(new DateLabels()).write(sheet), ShiftCsvWriter.CHARSET);
		String[] lines = csv.split("\r\n");

		assertEquals(6, lines.length);
		assertTrue(lines[0].startsWith(",11月度,,10/16~11/15,,16,17,18,19,"), lines[0]);
		assertTrue(lines[1].startsWith(",,,,,金,土,日,月,"), lines[1]);
		assertTrue(lines[2].startsWith("1,テストユーザー,変更もOK 土日休み,開始,5:00,,公,,,"), lines[2]);
		assertTrue(lines[2].endsWith(",30,1"), lines[2]);
		assertTrue(lines[3].startsWith(",10000001,,修了,8:00,,希,,クール,"), lines[3]);
		assertTrue(lines[4].startsWith(",,,出勤人数,,1,0,1,1,"), lines[4]);
		assertTrue(lines[5].startsWith(",,,予定人数,,15,15,"), lines[5]);
	}
}
