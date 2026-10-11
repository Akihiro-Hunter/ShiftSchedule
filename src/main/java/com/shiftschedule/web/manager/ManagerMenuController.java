package com.shiftschedule.web.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ManagerMenuController {

	@GetMapping("/manager/menu")
	public String menu() {
		return "manager/menu";
	}
}
