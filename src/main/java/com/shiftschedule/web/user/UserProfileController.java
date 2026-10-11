package com.shiftschedule.web.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.shiftschedule.security.LoginAccount;
import com.shiftschedule.service.AccountService;

/** 4.個人情報設定（個人情報の閲覧とパスワード変更） */
@Controller
@RequestMapping("/user/profile")
public class UserProfileController {

	private final AccountService accountService;

	public UserProfileController(AccountService accountService) {
		this.accountService = accountService;
	}

	@GetMapping
	public String show(@AuthenticationPrincipal LoginAccount login, Model model) {
		model.addAttribute("passwordForm", new PasswordForm());
		return render(login, model);
	}

	@PostMapping("/password")
	public String changePassword(@Validated PasswordForm form, BindingResult result,
			@AuthenticationPrincipal LoginAccount login, Model model, RedirectAttributes redirect) {
		if (!result.hasFieldErrors("newPassword") && !form.getNewPassword().equals(form.getConfirmPassword())) {
			result.rejectValue("confirmPassword", "mismatch", "新しいパスワード（確認）が一致しません");
		}
		if (!result.hasErrors()
				&& !accountService.changePassword(login.getId(), form.getCurrentPassword(), form.getNewPassword())) {
			result.rejectValue("currentPassword", "wrong", "現在のパスワードが正しくありません");
		}
		if (result.hasErrors()) {
			return render(login, model);
		}
		redirect.addFlashAttribute("message", "パスワードを変更しました");
		return "redirect:/user/profile";
	}

	private String render(LoginAccount login, Model model) {
		model.addAttribute("account", accountService.findById(login.getId()).orElseThrow());
		return "user/profile";
	}
}
