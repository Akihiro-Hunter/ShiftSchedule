package com.shiftschedule.controller.user;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.security.LoginAccount;
import com.shiftschedule.service.AccountService;

/** 3.可能作業 */
@Controller
@RequestMapping("/user/tasks")
public class UserTaskController {

	private final AccountService accountService;

	public UserTaskController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping
	public String show(@AuthenticationPrincipal LoginAccount login, Model model) {
		model.addAttribute("taskGroups", WorkTask.byGroup());
		model.addAttribute("tasks", accountService.findById(login.getId()).orElseThrow().getTasks());
		return "user/tasks";
	}

	@PostMapping
	public String save(@RequestParam(required = false) Set<WorkTask> tasks,
			@AuthenticationPrincipal LoginAccount login, RedirectAttributes redirect) {
		accountService.updateTasks(login.getId(), tasks == null ? new HashSet<>() : tasks);
		redirect.addFlashAttribute("message", "保存しました");
		return "redirect:/user/tasks";
	}
}
