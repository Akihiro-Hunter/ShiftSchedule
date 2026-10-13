package com.shiftschedule.controller.manager;

import java.util.function.Supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.service.TaskService;

/** 3.作業項目の編集（大項目と作業名の追加・名称変更・削除） */
@Controller
@RequestMapping("/manager/tasks")
public class ManagerTaskController {

	private static final String REDIRECT = "redirect:/manager/tasks";

	private final TaskService taskService;

	public ManagerTaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	@GetMapping
	public String show(Model model) {
		model.addAttribute("taskGroups", taskService.byGroup());
		model.addAttribute("userCounts", taskService.userCounts());
		return "manager/tasks";
	}

	@PostMapping("/groups")
	public String addGroup(@RequestParam String name, RedirectAttributes redirect) {
		return run(redirect, () -> "大項目「" + taskService.addGroup(name).getName() + "」を追加しました");
	}

	@PostMapping("/groups/{id}/rename")
	public String renameGroup(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
		return run(redirect, () -> {
			taskService.renameGroup(id, name);
			return "大項目名を変更しました";
		});
	}

	@PostMapping("/groups/{id}/delete")
	public String deleteGroup(@PathVariable Long id, RedirectAttributes redirect) {
		return run(redirect, () -> "大項目「" + taskService.deleteGroup(id) + "」を削除しました");
	}

	@PostMapping("/groups/{id}/tasks")
	public String addTask(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
		return run(redirect, () -> "作業「" + taskService.addTask(id, name).getName() + "」を追加しました");
	}

	@PostMapping("/{id}/rename")
	public String renameTask(@PathVariable Long id, @RequestParam String name, RedirectAttributes redirect) {
		return run(redirect, () -> {
			taskService.renameTask(id, name);
			return "作業名を変更しました";
		});
	}

	@PostMapping("/{id}/delete")
	public String deleteTask(@PathVariable Long id, RedirectAttributes redirect) {
		return run(redirect, () -> "作業「" + taskService.deleteTask(id) + "」を削除しました");
	}

	/** 入力エラー（重複・空欄など）は画面上のエラーメッセージにする */
	private String run(RedirectAttributes redirect, Supplier<String> action) {
		try {
			redirect.addFlashAttribute("message", action.get());
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return REDIRECT;
	}
}
