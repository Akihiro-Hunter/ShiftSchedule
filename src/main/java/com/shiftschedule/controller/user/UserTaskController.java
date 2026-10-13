package com.shiftschedule.controller.user;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.shiftschedule.service.TaskService;

/** 3.可能作業 */
@Controller
@RequestMapping("/user/tasks")
public class UserTaskController {

	private final AccountService accountService;
	private final TaskService taskService;

	public UserTaskController(AccountService accountService, TaskService taskService) {
		this.accountService = accountService;
		this.taskService = taskService;
	}

	@GetMapping
	public String show(@AuthenticationPrincipal LoginAccount login, Model model) {
		model.addAttribute("taskGroups", taskService.byGroup());
		model.addAttribute("taskIds", accountService.findById(login.getId()).orElseThrow().getTasks().stream()
				.map(WorkTask::getId).collect(Collectors.toSet()));
		return "user/tasks";
	}

	@PostMapping
	public String save(@RequestParam(required = false) Set<Long> taskIds,
			@AuthenticationPrincipal LoginAccount login, RedirectAttributes redirect) {
		accountService.updateTasks(login.getId(), taskService.resolve(taskIds == null ? new HashSet<>() : taskIds));
		redirect.addFlashAttribute("message", "保存しました");
		return "redirect:/user/tasks";
	}
}
