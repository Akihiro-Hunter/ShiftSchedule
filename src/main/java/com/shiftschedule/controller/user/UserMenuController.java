package com.shiftschedule.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserMenuController {

	@GetMapping("/user/menu")
	public String menu() {
		return "user/menu";
	}
}
