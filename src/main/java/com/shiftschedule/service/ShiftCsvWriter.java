package com.shiftschedule.service;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/** 朝アシシフトの CSV 出力（Excel で開けるよう Shift_JIS） */
@Component
public class ShiftCsvWriter {

	public static final Charset CHARSET = Charset.forName("Windows-31J");

	private final DateLabels dateLabels;

	public ShiftCsvWriter(DateLabels dateLabels) {
		this.dateLabels = dateLabels;
	}

	public byte[] write(ShiftSheet sheet) {
		StringBuilder sb = new StringBuilder();

		List<String> header = new ArrayList<>(List.of("ユーザー番号", "氏名"));
		for (LocalDate date : sheet.dates()) {
			header.add(dateLabels.monthDayWithWeek(date));
		}
		appendLine(sb, header);

		for (ShiftSheet.Row row : sheet.rows()) {
			List<String> top = new ArrayList<>(List.of(row.userNumber(), row.name()));
			List<String> bottom = new ArrayList<>(List.of("", ""));
			for (ShiftSheet.Cell cell : row.cells()) {
				top.add(cell.top());
				bottom.add(cell.bottom());
			}
			appendLine(sb, top);
			appendLine(sb, bottom);
		}
		return sb.toString().getBytes(CHARSET);
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
