package com.shiftschedule.web.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.shiftschedule.domain.Period;
import com.shiftschedule.security.LoginAccount;
import com.shiftschedule.service.ShiftService;
import com.shiftschedule.service.ShiftSheet;

/** 1.勤務表確認（確定済みの朝アシシフトを表示） */
@Controller
public class UserShiftController {

	private final ShiftService shiftService;

	public UserShiftController(ShiftService shiftService) {
		this.shiftService = shiftService;
	}

	/** 今期を表示。来期も確定済みなら p=next で切り替えられる */
	@GetMapping("/user/shift")
	public String show(@RequestParam(required = false) String p, @AuthenticationPrincipal LoginAccount login,
			Model model) {
		Period current = Period.current();
		boolean nextConfirmed = shiftService.isConfirmed(current.next());
		boolean showNext = "next".equals(p) && nextConfirmed;
		Period period = showNext ? current.next() : current;

		model.addAttribute("period", period);
		model.addAttribute("showNext", showNext);
		model.addAttribute("nextConfirmed", nextConfirmed);
		if (shiftService.isConfirmed(period)) {
			ShiftSheet sheet = shiftService.buildSheet(period);
			model.addAttribute("sheet", sheet);
			model.addAttribute("myRow", sheet.rows().stream()
					.filter(row -> row.accountId().equals(login.getId()))
					.findFirst().orElse(null));
		}
		return "user/shift";
	}
}
