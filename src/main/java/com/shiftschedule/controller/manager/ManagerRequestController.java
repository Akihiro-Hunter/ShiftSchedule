package com.shiftschedule.controller.manager;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.domain.Half;
import com.shiftschedule.domain.Period;
import com.shiftschedule.service.ShiftRequestService;
import com.shiftschedule.service.ShiftService;

/** 休み希望登録（口頭などで聞いた休み希望を、管理者がユーザーの代わりにまとめて登録する） */
@Controller
@RequestMapping("/manager/requests")
public class ManagerRequestController {

	private static final String OFF_PREFIX = "off_";

	private final ShiftRequestService requestService;
	private final ShiftService shiftService;

	public ManagerRequestController(ShiftRequestService requestService, ShiftService shiftService) {
		this.requestService = requestService;
		this.shiftService = shiftService;
	}

	/** start を省略した場合は来期を表示 */
	@GetMapping
	public String show(@RequestParam(required = false) String start, Model model) {
		Period period = Period.parseOrUpcoming(start);
		model.addAttribute("period", period);
		model.addAttribute("currentPeriod", Period.current());
		model.addAttribute("secondStart", period.start(Half.SECOND));
		model.addAttribute("staff", shiftService.staffInOrder());
		model.addAttribute("offDays", requestService.findOffDays(period));
		return "manager/requests";
	}

	/** フォーム値は off_{アカウントID}_{yyyy-MM-dd}（チェックされた日が休み希望） */
	@PostMapping
	public String save(@RequestParam String start, @RequestParam Map<String, String> params,
			RedirectAttributes redirect) {
		Period period = Period.parseOrUpcoming(start);
		Map<Long, Set<LocalDate>> offDays = new HashMap<>();
		params.keySet().forEach(name -> {
			if (!name.startsWith(OFF_PREFIX)) {
				return;
			}
			String[] parts = name.substring(OFF_PREFIX.length()).split("_", 2);
			if (parts.length != 2) {
				return;
			}
			try {
				offDays.computeIfAbsent(Long.valueOf(parts[0]), k -> new HashSet<>()).add(LocalDate.parse(parts[1]));
			} catch (RuntimeException e) {
				// 不正な値は無視する
			}
		});
		requestService.saveOffDays(period, offDays);
		redirect.addFlashAttribute("message", "保存しました");
		redirect.addAttribute("start", period.start().toString());
		return "redirect:/manager/requests";
	}
}
