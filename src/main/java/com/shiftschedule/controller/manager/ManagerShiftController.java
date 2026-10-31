package com.shiftschedule.controller.manager;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

import com.shiftschedule.domain.DailyPlan;
import com.shiftschedule.domain.Half;
import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;
import com.shiftschedule.domain.ShiftEntry;
import com.shiftschedule.service.ConsecutiveWork;
import com.shiftschedule.service.ShiftCsvWriter;
import com.shiftschedule.service.ShiftGenerateService;
import com.shiftschedule.service.ShiftService;
import com.shiftschedule.service.ShiftSheet;

/** 2.ユーザー勤務表管理（朝アシシフトの作成・確定・CSV出力） */
@Controller
@RequestMapping("/manager/shifts")
public class ManagerShiftController {

	private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final ShiftService shiftService;
	private final ShiftCsvWriter csvWriter;
	private final ShiftGenerateService generateService;

	public ManagerShiftController(ShiftService shiftService, ShiftCsvWriter csvWriter,
			ShiftGenerateService generateService) {
		this.shiftService = shiftService;
		this.csvWriter = csvWriter;
		this.generateService = generateService;
	}

	/** start を省略した場合は来期を表示 */
	@GetMapping
	public String show(@RequestParam(required = false) String start, Model model) {
		Period period = Period.parseOrUpcoming(start);
		ShiftSheet sheet = shiftService.buildSheet(period);
		model.addAttribute("sheet", sheet);
		model.addAttribute("assignmentView", generateService.buildView(period, sheet));
		model.addAttribute("period", period);
		model.addAttribute("currentPeriod", Period.current());
		model.addAttribute("halves", Half.values());
		model.addAttribute("secondStart", period.start(Half.SECOND));
		model.addAttribute("maxRun", ConsecutiveWork.MAX_DAYS);
		model.addAttribute("statuses", RequestStatus.values());
		model.addAttribute("noteMaxLength", ShiftEntry.NOTE_MAX_LENGTH);
		model.addAttribute("planMaxCount", DailyPlan.MAX_COUNT);
		return "manager/shifts";
	}

	/**
	 * @param order ドラッグで並び替えたユーザーの順（アカウントID）
	 */
	@PostMapping
	public String save(@RequestParam String start, @RequestParam(defaultValue = "save") String action,
			@RequestParam(required = false) List<Long> order, @RequestParam Map<String, String> params,
			RedirectAttributes redirect) {
		Period period = Period.parseOrUpcoming(start);
		if (order != null) {
			shiftService.saveOrder(order);
		}
		shiftService.savePlans(period, params);
		shiftService.saveEntries(period, params);
		if (action.equals("generate")) {
			// 鍵の状態を先に保存し、鍵のかかった人を作り直さないようにする
			shiftService.saveLocks(period, params);
			return generate(period, redirect);
		}
		// action は "save"、または "confirm-FIRST" / "unconfirm-SECOND" のように半月を指定する
		String[] parts = action.split("-", 2);
		Half half = parts.length == 2 ? parseHalf(parts[1]) : null;
		if (half != null && parts[0].equals("confirm")) {
			shiftService.setConfirmed(period, half, true);
			redirect.addFlashAttribute("message",
					half.getLabel() + "を保存して確定しました。ユーザーの勤務表確認に表示されます");
			// 連勤は前半・後半をまたいで数えるので、確定する半月に7連勤以上の人がいれば知らせる（確定は止めない）
			List<String> names = shiftService.buildSheet(period).overLimitNames(half);
			if (!names.isEmpty()) {
				redirect.addFlashAttribute("warning", "7連勤以上の人がいます：" + String.join("、", names));
			}
		} else if (half != null && parts[0].equals("unconfirm")) {
			shiftService.setConfirmed(period, half, false);
			redirect.addFlashAttribute("message", half.getLabel() + "の確定を取り消しました。ユーザーには表示されなくなります");
		} else {
			redirect.addFlashAttribute("message", "保存しました");
		}
		// 鍵の状態は勤怠の保存の後に反映する（鍵をかける直前までの入力は保存する）
		shiftService.saveLocks(period, params);
		redirect.addAttribute("start", period.start().toString());
		return "redirect:/manager/shifts";
	}

	/** 条件に沿って勤務表を作り直し、足りない作業があれば知らせる（確定は止めない） */
	private String generate(Period period, RedirectAttributes redirect) {
		try {
			ShiftGenerateService.Outcome outcome = generateService.generate(period);
			redirect.addFlashAttribute("message",
					"勤務表を作成しました。内容を確認して、必要なら修正してから保存・確定してください");
			List<String> warnings = new java.util.ArrayList<>();
			if (outcome.isUnderstaffed()) {
				warnings.add("契約日数の範囲では、予定人数に届きません（出勤できる延べ人数の目安 " + outcome.capacity() + "、予定人数の合計 "
						+ outcome.planned() + "）。人手が足りない日は、募集や契約日数の見直しが必要です");
			}
			if (!outcome.optimal()) {
				warnings.add("制限時間内に最良の案を探しきれませんでした（見つかった中で一番良い案を使っています）。もう一度作成すると改善することがあります");
			}
			List<String> shortages = generateService.buildView(period, shiftService.buildSheet(period)).shortages();
			if (!shortages.isEmpty()) {
				int shown = Math.min(shortages.size(), 5);
				warnings.add("必須の作業が足りない日があります（" + shortages.size() + "件）："
						+ String.join("、", shortages.subList(0, shown)) + (shortages.size() > shown ? " ほか" : ""));
			}
			if (!warnings.isEmpty()) {
				redirect.addFlashAttribute("warning", String.join(" ／ ", warnings));
			}
		} catch (IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		redirect.addAttribute("start", period.start().toString());
		return "redirect:/manager/shifts";
	}

	private static Half parseHalf(String value) {
		try {
			return Half.valueOf(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
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
