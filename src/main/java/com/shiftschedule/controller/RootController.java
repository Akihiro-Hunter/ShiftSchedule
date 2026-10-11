package com.shiftschedule.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

	@GetMapping("/")
	public String root() {
		return "redirect:/user/login";
	}

	@GetMapping("/manager")
	public String managerRoot() {
		return "redirect:/manager/menu";
	}

	@GetMapping("/user")
	public String userRoot() {
		return "redirect:/user/menu";
	}

	/** ログイン画面（見た目は共通で、送信先だけ /manager と /user で異なる） */
	@GetMapping("/manager/login")
	public String managerLogin(Model model) {
		model.addAttribute("area", "manager");
		return "login";
	}

	@GetMapping("/user/login")
	public String userLogin(Model model) {
		model.addAttribute("area", "user");
		return "login";
	}
}
