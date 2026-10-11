package com.shiftschedule.controller.user;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shiftschedule.domain.Period;
import com.shiftschedule.security.LoginAccount;
import com.shiftschedule.service.ShiftPdfWriter;
import com.shiftschedule.service.ShiftService;
import com.shiftschedule.service.ShiftSheet;

/** 1.勤務表確認（確定済みの朝アシシフトを表示） */
@Controller
public class UserShiftController {

	private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ShiftService shiftService;
	private final ShiftPdfWriter pdfWriter;

	public UserShiftController(ShiftService shiftService, ShiftPdfWriter pdfWriter) {
		this.shiftService = shiftService;
		this.pdfWriter = pdfWriter;
	}

	/** 今期を表示。来期も確定済みなら p=next で切り替えられる */
	@GetMapping("/user/shift")
	public String show(@RequestParam(required = false) String p, @AuthenticationPrincipal LoginAccount login,
			Model model) {
		Period current = Period.current();
		boolean nextConfirmed = shiftService.isAnyConfirmed(current.next());
		boolean showNext = "next".equals(p) && nextConfirmed;
		Period period = showNext ? current.next() : current;

		model.addAttribute("period", period);
		model.addAttribute("showNext", showNext);
		model.addAttribute("nextConfirmed", nextConfirmed);
		if (shiftService.isAnyConfirmed(period)) {
			// 確定済みの半月だけを見せる
			ShiftSheet sheet = shiftService.buildSheet(period).onlyConfirmed();
			model.addAttribute("sheet", sheet);
			model.addAttribute("myRow", sheet.rows().stream()
					.filter(row -> row.accountId().equals(login.getId()))
					.findFirst().orElse(null));
		}
		return "user/shift";
	}

	/** 朝アシシフト（全員）の PDF（A4横）。確定していない期間は表示しない */
	@GetMapping("/user/shift/pdf")
	public ResponseEntity<byte[]> pdf(@RequestParam(required = false) String p,
			@AuthenticationPrincipal LoginAccount login) {
		Period period = "next".equals(p) ? Period.current().next() : Period.current();
		if (!shiftService.isAnyConfirmed(period)) {
			return ResponseEntity.notFound().build();
		}
		ShiftSheet sheet = shiftService.buildSheet(period).onlyConfirmed();
		String fileName = "朝アシシフト_" + FILE_DATE.format(period.start()) + "-" + FILE_DATE.format(period.end())
				+ ".pdf";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
				.contentType(MediaType.APPLICATION_PDF)
				.body(pdfWriter.write(sheet, login.getId()));
	}
}
