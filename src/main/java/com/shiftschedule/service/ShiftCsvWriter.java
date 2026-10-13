package com.shiftschedule.service;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.shiftschedule.domain.Period;

/**
 * 朝アシシフトの CSV 出力（Excel で開けるよう Shift_JIS）。
 * 紙の勤務表と同じ並び：No・氏名／ユーザー番号・備考・開始／修了と時刻・各日の上段／下段・出勤回数・公年休数、
 * 最後に日ごとの出勤人数と予定人数。
 */
@Component
public class ShiftCsvWriter {

	public static final Charset CHARSET = Charset.forName("Windows-31J");

	/** 日付の列より左の列数（No・氏名・備考・開始/修了・時刻） */
	private static final int LEFT_COLUMNS = 5;

	private final DateLabels dateLabels;

	public ShiftCsvWriter(DateLabels dateLabels) {
		this.dateLabels = dateLabels;
	}

	public byte[] write(ShiftSheet sheet) {
		StringBuilder sb = new StringBuilder();
		Period period = sheet.period();

		// 見出し：期間（例：11月度、10/16~11/15）と日付・曜日
		List<String> dayRow = new ArrayList<>(List.of("", period.end().getMonthValue() + "月度", "",
				shortDate(period.start()) + "~" + shortDate(period.end()), ""));
		List<String> weekRow = blanks(LEFT_COLUMNS);
		for (LocalDate date : sheet.dates()) {
			dayRow.add(String.valueOf(date.getDayOfMonth()));
			weekRow.add(dateLabels.week(date));
		}
		dayRow.addAll(List.of("出勤回数", "公年休数"));
		appendLine(sb, dayRow);
		appendLine(sb, weekRow);

		// 1ユーザー2行（上段：「公」、下段：「希」や作業など）
		int no = 1;
		for (ShiftSheet.Row row : sheet.rows()) {
			List<String> top = new ArrayList<>(
					List.of(String.valueOf(no++), row.name(), oneLine(row.requestNote()), "開始", row.startTime()));
			List<String> bottom = new ArrayList<>(List.of("", row.userNumber(), "", "修了", row.endTime()));
			for (ShiftSheet.Cell cell : row.cells()) {
				top.add(cell.top());
				bottom.add(cell.bottom());
			}
			top.add(String.valueOf(row.workDays()));
			top.add(String.valueOf(row.dayOffDays()));
			appendLine(sb, top);
			appendLine(sb, bottom);
		}

		// 日ごとの合計
		List<String> workers = blanks(LEFT_COLUMNS - 2);
		workers.addAll(List.of("出勤人数", ""));
		sheet.workerCounts().forEach(count -> workers.add(String.valueOf(count)));
		List<String> planned = blanks(LEFT_COLUMNS - 2);
		planned.addAll(List.of("予定人数", ""));
		sheet.plannedCounts().forEach(count -> planned.add(String.valueOf(count)));
		appendLine(sb, workers);
		appendLine(sb, planned);

		return sb.toString().getBytes(CHARSET);
	}

	private static String shortDate(LocalDate date) {
		return date.getMonthValue() + "/" + date.getDayOfMonth();
	}

	/** 備考は1行にまとめる */
	private static String oneLine(String value) {
		return value == null ? "" : value.strip().replaceAll("\\s*\\R\\s*", " ");
	}

	private static List<String> blanks(int count) {
		return new ArrayList<>(Collections.nCopies(count, ""));
	}

	private static void appendLine(StringBuilder sb, List<String> values) {
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(escape(values.get(i)));
		}
		sb.append("\r\n");
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}
