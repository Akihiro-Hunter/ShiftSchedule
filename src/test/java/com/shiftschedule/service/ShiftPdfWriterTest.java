package com.shiftschedule.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.shiftschedule.domain.Period;

class ShiftPdfWriterTest {

	private static final String FONT = "C:/Windows/Fonts/msgothic.ttc";

	@Test
	void A4横で全員分を出力し多い場合は改ページする() throws Exception {
		assumeTrue(new File(FONT).isFile(), "日本語フォントがない環境ではスキップ");
		Period period = new Period(LocalDate.of(2026, 9, 16));
		List<ShiftSheet.Row> rows = new ArrayList<>();
		for (long id = 1; id <= 40; id++) {
			List<ShiftSheet.Cell> cells = new ArrayList<>();
			for (LocalDate d : period.dates()) {
				boolean off = (d.getDayOfMonth() + id) % 5 == 0;
				cells.add(new ShiftSheet.Cell(d, null, off, off ? "希" : "クール", off));
			}
			rows.add(new ShiftSheet.Row(id, String.format("%08d", id), "ユーザー" + id, "", cells, null, null, false, 0));
		}
		ShiftSheet sheet = new ShiftSheet(period, period.dates(), rows, List.of(), new ShiftSheet.HalfStatus(true, null),
				new ShiftSheet.HalfStatus(true, null));

		byte[] pdf = new ShiftPdfWriter(new DateLabels(), FONT).write(sheet, 2L);

		try (PDDocument doc = Loader.loadPDF(pdf)) {
			assertTrue(doc.getNumberOfPages() >= 2);
			assertEquals(842, Math.round(doc.getPage(0).getMediaBox().getWidth()));
			String text = new PDFTextStripper().getText(doc);
			assertTrue(text.contains("朝アシシフト"));
			assertTrue(text.contains("ユーザー40"));
		}
	}
}
