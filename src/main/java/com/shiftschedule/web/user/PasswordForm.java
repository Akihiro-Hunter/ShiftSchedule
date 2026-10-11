package com.shiftschedule.web.user;

import com.shiftschedule.service.AccountRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** パスワード変更フォーム */
public class PasswordForm {

	@NotBlank(message = "現在のパスワードを入力してください")
	private String currentPassword;

	@NotNull(message = AccountRules.PASSWORD_MESSAGE)
	@Pattern(regexp = AccountRules.PASSWORD_REGEX, message = AccountRules.PASSWORD_MESSAGE)
	private String newPassword;

	private String confirmPassword;

	public String getCurrentPassword() {
		return currentPassword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}
}
