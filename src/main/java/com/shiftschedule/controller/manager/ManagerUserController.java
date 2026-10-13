package com.shiftschedule.controller.manager;

import java.util.Objects;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.shiftschedule.domain.Role;
import com.shiftschedule.form.AccountForm;
import com.shiftschedule.security.LoginAccount;
import com.shiftschedule.service.AccountRules;
import com.shiftschedule.service.AccountService;
import com.shiftschedule.service.TaskService;

/** 1.ユーザー情報管理 */
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
		model.addAttribute("allRoles", Role.values());
		model.addAttribute("allPriorities", Priority.values());
	}

	@GetMapping
	public String list(Model model) {
		model.addAttribute("accounts", accountService.findAll());
		return "manager/users";
	}

	@GetMapping("/new")
	public String newForm(Model model) {
		model.addAttribute("accountForm", new AccountForm());
		return "manager/user-form";
	}

	@GetMapping("/{id}")
	public String editForm(@PathVariable Long id, Model model) {
		Account account = accountService.findById(id).orElseThrow();
		model.addAttribute("accountForm", AccountForm.from(account));
		return "manager/user-form";
	}

	@PostMapping("/save")
	public String save(@Validated AccountForm form, BindingResult result,
			@AuthenticationPrincipal LoginAccount login, RedirectAttributes redirect) {
		validate(form, result, login);
		if (result.hasErrors()) {
			return "manager/user-form";
		}
		Account account = form.isNew() ? new Account() : accountService.findById(form.getId()).orElseThrow();
		form.applyTo(account);
		account.setTasks(taskService.resolve(form.getTaskIds()));
		accountService.save(account, form.getPassword());
		redirect.addFlashAttribute("message", account.getName() + " さんの情報を保存しました");
		return "redirect:/manager/users";
	}

	@PostMapping("/{id}/delete")
	public String delete(@PathVariable Long id, @AuthenticationPrincipal LoginAccount login,
			RedirectAttributes redirect) {
		if (id.equals(login.getId())) {
			redirect.addFlashAttribute("error", "ログイン中の管理者自身は削除できません");
			return "redirect:/manager/users";
		}
		Account account = accountService.findById(id).orElseThrow();
		accountService.delete(id);
		redirect.addFlashAttribute("message", account.getName() + " さんを削除しました");
		return "redirect:/manager/users";
	}

	private void validate(AccountForm form, BindingResult result, LoginAccount login) {
		if (!result.hasFieldErrors("userNumber")
				&& accountService.isUserNumberTaken(form.getUserNumber(), form.getId())) {
			result.rejectValue("userNumber", "duplicate", "このユーザー番号は既に使われています");
		}
		String password = form.getPassword();
		if (form.isNew() && (password == null || password.isEmpty())) {
			result.rejectValue("password", "required", "初期パスワードを入力してください");
		} else if (password != null && !password.isEmpty() && !AccountRules.isValidPassword(password)) {
			result.rejectValue("password", "pattern", AccountRules.PASSWORD_MESSAGE);
		}
		if (form.getRole().isStaff()) {
			if (form.getBirthDate() == null) {
				result.rejectValue("birthDate", "required", "生年月日を入力してください");
			}
			if (form.getHireDate() == null) {
				result.rejectValue("hireDate", "required", "入社日を入力してください");
			}
		}
		if (Objects.equals(form.getId(), login.getId()) && form.getRole() != Role.MANAGER) {
			result.rejectValue("role", "self", "ログイン中の管理者自身の役職は変更できません");
		}
	}
}
