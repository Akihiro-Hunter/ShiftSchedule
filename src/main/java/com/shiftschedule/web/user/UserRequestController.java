package com.shiftschedule.web.user;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;
import com.shiftschedule.domain.ShiftRequestNote;
import com.shiftschedule.security.LoginAccount;
import com.shiftschedule.service.ShiftRequestService;

/** 2.次回勤務表作成（来期の出勤可能日と備考の入力） */
@Controller
@RequestMapping("/user/request")
public class UserRequestController {

	private static final String STATUS_PREFIX = "status_";

	private final ShiftRequestService requestService;

	public UserRequestController(ShiftRequestService requestService) {
		this.requestService = requestService;
	}

	@GetMapping
	public String show(@AuthenticationPrincipal LoginAccount login, Model model) {
		Period period = Period.upcoming();
		Map<LocalDate, RequestStatus> statuses = requestService.findStatuses(login.getId(), period);
		ShiftRequestNote note = requestService.findNote(login.getId(), period).orElse(null);

		model.addAttribute("period", period);
		model.addAttribute("statuses", statuses);
		model.addAttribute("allStatuses", RequestStatus.values());
		model.addAttribute("note", note == null ? "" : note.getNote());
		model.addAttribute("updatedAt", note == null ? null : note.getUpdatedAt());
		model.addAttribute("unansweredCount", period.dates().size() - statuses.size());
		model.addAttribute("noteMaxLength", ShiftRequestNote.NOTE_MAX_LENGTH);
		return "user/request";
	}

	@PostMapping
	public String save(@RequestParam Map<String, String> params, @AuthenticationPrincipal LoginAccount login,
			RedirectAttributes redirect) {
		Period period = Period.upcoming();
		Map<LocalDate, RequestStatus> statuses = new HashMap<>();
		params.forEach((name, value) -> {
			if (!name.startsWith(STATUS_PREFIX)) {
				return;
			}
			try {
				statuses.put(LocalDate.parse(name.substring(STATUS_PREFIX.length())), RequestStatus.valueOf(value));
			} catch (RuntimeException e) {
				// 不正な値は無視する
			}
		});
		requestService.save(login.getId(), period, statuses, params.get("note"));
		redirect.addFlashAttribute("message", "保存しました");
		return "redirect:/user/request";
	}
}
