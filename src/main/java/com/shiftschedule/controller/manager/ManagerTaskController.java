package com.shiftschedule.controller.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.domain.RequirementKind;
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
		model.addAttribute("kinds", RequirementKind.values());
		model.addAttribute("countMax", TaskService.REQUIRED_MAX);
		return "manager/tasks";
	}

	@PostMapping("/add")
	public String add(@RequestParam String groupName, @RequestParam(defaultValue = "") String taskName, RedirectAttributes redirect) {
		return run(redirect, () -> taskService.addEntry(groupName, taskName));
	}

	/** 画面の全入力欄（g_{大項目ID} / t_{作業ID} / k_{作業ID}=種別 / n_{作業ID}=必要人数）をまとめて保存する */
	@PostMapping("/save")
	public String save(@RequestParam Map<String, String> params, RedirectAttributes redirect) {
		Map<Long, String> groupNames = new HashMap<>();
		Map<Long, String> taskNames = new HashMap<>();
		Map<Long, String> kinds = new HashMap<>();
		Map<Long, String> counts = new HashMap<>();
		params.forEach((key, value) -> {
			if (key.startsWith("g_")) {
				groupNames.put(Long.valueOf(key.substring(2)), value);
			} else if (key.startsWith("t_")) {
				taskNames.put(Long.valueOf(key.substring(2)), value);
			} else if (key.startsWith("k_")) {
				kinds.put(Long.valueOf(key.substring(2)), value);
			} else if (key.startsWith("n_")) {
				counts.put(Long.valueOf(key.substring(2)), value);
			}
		});
		return run(redirect, () -> {
			taskService.saveNames(groupNames, taskNames, kinds, counts);
			return "保存しました";
		});
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
