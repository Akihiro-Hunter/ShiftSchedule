package com.shiftschedule.controller.manager;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Priority;
import com.shiftschedule.form.UserEditForm;
import com.shiftschedule.service.AccountService;
import com.shiftschedule.service.TaskService;

/** 1.ユーザー情報管理（優先度・残年次有給休暇数・可能作業の編集） */
@Controller
@RequestMapping("/manager/users")
public class ManagerUserController {

	private final AccountService accountService;

	private final TaskService taskService;

	public ManagerUserController(AccountService accountService, TaskService taskService) {
		this.accountService = accountService;
		this.taskService = taskService;
	}

	@ModelAttribute
	public void common(Model model) {
		model.addAttribute("taskGroups", taskService.byGroup());
		model.addAttribute("allPriorities", Priority.values());
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("accounts", accountService.findAll());
		return "manager/users";
	}

	@GetMapping("/{id}")
	public String editForm(@PathVariable Long id, Model model) {
		Account account = accountService.findById(id).orElseThrow();
		model.addAttribute("userEditForm", UserEditForm.from(account));
		return "manager/user-form";
	}

	@PostMapping("/save")
	public String save(@Validated UserEditForm form, BindingResult result, RedirectAttributes redirect) {
		Account account = accountService.findById(form.getId()).orElseThrow();
		if (result.hasErrors()) {
			// 表示専用の項目はフォームに含まれないため、保存済みの値を戻す
			form.setUserNumber(account.getUserNumber());
			form.setName(account.getName());
			return "manager/user-form";
		}
		form.applyTo(account);
		account.setTasks(taskService.resolve(form.getTaskIds()));
		accountService.save(account, null);
		redirect.addFlashAttribute("message", account.getName() + " さんの情報を保存しました");
		return "redirect:/manager/users";
	}
}
