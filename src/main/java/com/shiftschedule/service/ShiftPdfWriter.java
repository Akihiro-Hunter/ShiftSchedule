package com.shiftschedule.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 朝アシシフトの PDF 出力（A4横）。
 * 日本語フォントは app.pdf.font で指定する。未指定なら Windows / Linux の標準的な場所から探す。
 */
@Component
public class ShiftPdfWriter {

	/** TrueType のフォントのみ（OpenType の CFF 形式は PDFBox で埋め込めない） */
	private static final List<String> FONT_CANDIDATES = List.of(
			"C:/Windows/Fonts/msgothic.ttc",
			"C:/Windows/Fonts/meiryo.ttc",
			"/usr/share/fonts/opentype/ipafont-gothic/ipag.ttf",
			"/usr/share/fonts/truetype/fonts-japanese-gothic.ttf");

	private static final PDRectangle PAGE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
	private static final float MARGIN = 24;
	private static final float TITLE_HEIGHT = 26;
	private static final float HEADER_HEIGHT = 24;
	private static final float ROW_HEIGHT = 15;
	private static final float NAME_WIDTH = 92;
	private static final float FOOTER_HEIGHT = 14;

	private static final Color TEXT = new Color(0x1f2933);
	private static final Color MUTED = new Color(0x6b7680);
	private static final Color LINE = new Color(0xaab3bc);
	private static final Color HEADER_BG = new Color(0xf0f3f6);
	private static final Color ME_BG = new Color(0xeef4ff);
	private static final Color SAT = new Color(0x1f6feb);
	private static final Color SUN = new Color(0xd1435b);

	private final DateLabels dateLabels;
	private final String fontPath;

	public ShiftPdfWriter(DateLabels dateLabels, @Value("${app.pdf.font:}") String fontPath) {
		this.dateLabels = dateLabels;
		this.fontPath = fontPath;
	}

	/**
	 * @param highlightAccountId 背景色を付けるユーザー（閲覧している本人。不要なら null）
	 */
	public byte[] write(ShiftSheet sheet, Long highlightAccountId) {
		try (PDDocument doc = new PDDocument(); FontSource source = openFont()) {
			PDFont font = PDType0Font.load(doc, source.font(), true);
			List<LocalDate> dates = sheet.dates();
			float cellWidth = (PAGE.getWidth() - MARGIN * 2 - NAME_WIDTH) / dates.size();
			float tableTop = PAGE.getHeight() - MARGIN - TITLE_HEIGHT;
			int usersPerPage = Math.max(1,
					(int) ((tableTop - HEADER_HEIGHT - MARGIN - FOOTER_HEIGHT) / (ROW_HEIGHT * 2)));

			List<List<ShiftSheet.Row>> pages = new ArrayList<>();
			List<ShiftSheet.Row> rows = sheet.rows();
			for (int i = 0; i < rows.size() || pages.isEmpty(); i += usersPerPage) {
				pages.add(rows.subList(Math.min(i, rows.size()), Math.min(i + usersPerPage, rows.size())));
			}

			for (int p = 0; p < pages.size(); p++) {
				PDPage page = new PDPage(PAGE);
				doc.addPage(page);
				try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
					Canvas canvas = new Canvas(cs, font);
					canvas.text("朝アシシフト　" + sheet.period().label(), MARGIN, PAGE.getHeight() - MARGIN - 12, 13,
							TEXT);
					drawHeader(canvas, dates, tableTop, cellWidth);
					float y = tableTop - HEADER_HEIGHT;
					for (ShiftSheet.Row row : pages.get(p)) {
						drawRow(canvas, row, y, cellWidth, row.accountId().equals(highlightAccountId));
						y -= ROW_HEIGHT * 2;
					}
					canvas.text((p + 1) + " / " + pages.size(), PAGE.getWidth() / 2 - 10, MARGIN - 4, 8, MUTED);
				}
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			doc.save(out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void drawHeader(Canvas canvas, List<LocalDate> dates, float top, float cellWidth) throws IOException {
		float bottom = top - HEADER_HEIGHT;
		canvas.fill(MARGIN, bottom, NAME_WIDTH + cellWidth * dates.size(), HEADER_HEIGHT, HEADER_BG);
		canvas.text("氏名", MARGIN + 4, bottom + 9, 9, TEXT);
		for (int i = 0; i < dates.size(); i++) {
			LocalDate date = dates.get(i);
			float x = MARGIN + NAME_WIDTH + cellWidth * i;
			Color color = weekColor(date);
			canvas.centered(String.valueOf(date.getDayOfMonth()), x, cellWidth, bottom + 13, 9, color);
			canvas.centered(dateLabels.week(date), x, cellWidth, bottom + 3, 7, color);
		}
		canvas.grid(MARGIN, bottom, NAME_WIDTH, cellWidth, dates.size(), HEADER_HEIGHT, 1);
	}

	private void drawRow(Canvas canvas, ShiftSheet.Row row, float top, float cellWidth, boolean highlight)
			throws IOException {
		List<ShiftSheet.Cell> cells = row.cells();
		float bottom = top - ROW_HEIGHT * 2;
		if (highlight) {
			canvas.fill(MARGIN, bottom, NAME_WIDTH + cellWidth * cells.size(), ROW_HEIGHT * 2, ME_BG);
		}
		canvas.fitted(row.name(), MARGIN + 4, NAME_WIDTH - 8, bottom + ROW_HEIGHT - 4, 10, TEXT);
		for (int i = 0; i < cells.size(); i++) {
			ShiftSheet.Cell cell = cells.get(i);
			float x = MARGIN + NAME_WIDTH + cellWidth * i;
			canvas.centered(cell.top(), x, cellWidth, top - ROW_HEIGHT + 4, 9, SUN);
			canvas.centeredFitted(cell.bottom(), x, cellWidth, bottom + 4, 8, TEXT);
		}
		// 名前は2段ぶち抜き、日付のマスは上下2段
		canvas.line(MARGIN + NAME_WIDTH, top - ROW_HEIGHT, MARGIN + NAME_WIDTH + cellWidth * cells.size(),
				top - ROW_HEIGHT, 0.5f);
		canvas.grid(MARGIN, bottom, NAME_WIDTH, cellWidth, cells.size(), ROW_HEIGHT * 2, 1);
	}

	private static Color weekColor(LocalDate date) {
		DayOfWeek dow = date.getDayOfWeek();
		if (dow == DayOfWeek.SATURDAY) {
			return SAT;
		}
		if (dow == DayOfWeek.SUNDAY) {
			return SUN;
		}
		return TEXT;
	}

	private FontSource openFont() throws IOException {
		List<String> paths = fontPath.isBlank() ? FONT_CANDIDATES : List.of(fontPath);
		for (String path : paths) {
			File file = new File(path);
			if (file.isFile()) {
				return FontSource.open(file);
			}
		}
		throw new IllegalStateException("PDF用の日本語フォントが見つかりません。"
				+ "config/application.properties の app.pdf.font に TrueType フォント（.ttf / .ttc）のパスを設定してください。");
	}

	/** 読み込んだフォント。.ttc の場合は先頭のフォントを使い、PDF保存後に閉じる */
	private record FontSource(TrueTypeFont font, Closeable owner) implements Closeable {

		static FontSource open(File file) throws IOException {
			if (file.getName().toLowerCase().endsWith(".ttc")) {
				TrueTypeCollection collection = new TrueTypeCollection(file);
				List<TrueTypeFont> fonts = new ArrayList<>();
				collection.processAllFonts(fonts::add);
				return new FontSource(fonts.getFirst(), collection);
			}
			TrueTypeFont font = new TTFParser().parse(new RandomAccessReadBufferedFile(file));
			return new FontSource(font, font);
		}

		@Override
		public void close() throws IOException {
			owner.close();
		}
	}

	/** 描画の小さなヘルパー */
	private record Canvas(PDPageContentStream cs, PDFont font) {

		void text(String text, float x, float y, float size, Color color) throws IOException {
			if (text == null || text.isEmpty()) {
				return;
			}
			cs.beginText();
			cs.setFont(font, size);
			cs.setNonStrokingColor(color);
			cs.newLineAtOffset(x, y);
			cs.showText(text);
			cs.endText();
		}

		float width(String text, float size) throws IOException {
			return font.getStringWidth(text) / 1000 * size;
		}

		/** 幅に収まらなければ文字を小さくする */
		void fitted(String text, float x, float maxWidth, float y, float size, Color color) throws IOException {
			if (text == null || text.isEmpty()) {
				return;
			}
			float w = width(text, size);
			text(text, x, y, w > maxWidth ? size * maxWidth / w : size, color);
		}

		void centered(String text, float x, float cellWidth, float y, float size, Color color) throws IOException {
			if (text == null || text.isEmpty()) {
				return;
			}
			text(text, x + (cellWidth - width(text, size)) / 2, y, size, color);
		}

		void centeredFitted(String text, float x, float cellWidth, float y, float size, Color color)
				throws IOException {
			if (text == null || text.isEmpty()) {
				return;
			}
			float maxWidth = cellWidth - 2;
			float w = width(text, size);
			float s = w > maxWidth ? size * maxWidth / w : size;
			centered(text, x, cellWidth, y, s, color);
		}

		void fill(float x, float y, float w, float h, Color color) throws IOException {
			cs.setNonStrokingColor(color);
			cs.addRect(x, y, w, h);
			cs.fill();
		}

		void line(float x1, float y1, float x2, float y2, float width) throws IOException {
			cs.setStrokingColor(LINE);
			cs.setLineWidth(width);
			cs.moveTo(x1, y1);
			cs.lineTo(x2, y2);
			cs.stroke();
		}

		/** 氏名列＋日付列の枠線。外枠は太さ outer */
		void grid(float x, float y, float nameWidth, float cellWidth, int columns, float height, float outer)
				throws IOException {
			float right = x + nameWidth + cellWidth * columns;
			for (int i = 1; i < columns; i++) {
				float cx = x + nameWidth + cellWidth * i;
				line(cx, y, cx, y + height, 0.5f);
			}
			cs.setStrokingColor(LINE);
			cs.setLineWidth(outer);
			cs.addRect(x, y, right - x, height);
			cs.stroke();
			line(x + nameWidth, y, x + nameWidth, y + height, outer);
		}
	}
}
