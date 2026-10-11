package com.shiftschedule.web.manager;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;
import com.shiftschedule.domain.ShiftEntry;
import com.shiftschedule.service.ShiftCsvWriter;
import com.shiftschedule.service.ShiftService;
import com.shiftschedule.service.ShiftSheet;

/** 2.ユーザー勤務表管理（朝アシシフトの作成・確定・CSV出力） */
@Controller
@RequestMapping("/manager/shifts")
public class ManagerShiftController {

	private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ShiftService shiftService;
	private final ShiftCsvWriter csvWriter;

	public ManagerShiftController(ShiftService shiftService, ShiftCsvWriter csvWriter) {
		this.shiftService = shiftService;
		this.csvWriter = csvWriter;
	}

	/** start を省略した場合は来期を表示 */
	@GetMapping
	public String show(@RequestParam(required = false) String start, Model model) {
		Period period = Period.parseOrUpcoming(start);
		model.addAttribute("sheet", shiftService.buildSheet(period));
		model.addAttribute("period", period);
		model.addAttribute("currentPeriod", Period.current());
		model.addAttribute("statuses", RequestStatus.values());
		model.addAttribute("noteMaxLength", ShiftEntry.NOTE_MAX_LENGTH);
		return "manager/shifts";
	}

	@PostMapping
	public String save(@RequestParam String start, @RequestParam(defaultValue = "save") String action,
			@RequestParam Map<String, String> params, RedirectAttributes redirect) {
		Period period = Period.parseOrUpcoming(start);
		switch (action) {
		case "confirm" -> {
			shiftService.saveEntries(period, params);
			shiftService.setConfirmed(period, true);
			redirect.addFlashAttribute("message", "保存して確定しました。ユーザーの勤務表確認に表示されます");
		}
		case "unconfirm" -> {
			shiftService.saveEntries(period, params);
			shiftService.setConfirmed(period, false);
			redirect.addFlashAttribute("message", "確定を取り消しました。ユーザーには表示されなくなります");
		}
		default -> {
			shiftService.saveEntries(period, params);
			redirect.addFlashAttribute("message", "保存しました");
		}
		}
		redirect.addAttribute("start", period.start().toString());
		return "redirect:/manager/shifts";
	}

	@GetMapping("/csv")
	public ResponseEntity<byte[]> csv(@RequestParam(required = false) String start) {
		Period period = Period.parseOrUpcoming(start);
		ShiftSheet sheet = shiftService.buildSheet(period);
		String fileName = "朝アシシフト_" + FILE_DATE.format(period.start()) + "-" + FILE_DATE.format(period.end())
				+ ".csv";
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
				.contentType(new MediaType("text", "csv", ShiftCsvWriter.CHARSET))
				.body(csvWriter.write(sheet));
	}
}
