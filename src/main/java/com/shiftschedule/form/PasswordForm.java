package com.shiftschedule.form;

import com.shiftschedule.service.AccountRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** パスワード変更フォーム */
@Data
public class PasswordForm {

	@NotBlank(message = "現在のパスワードを入力してください")
	private String currentPassword;

	@NotNull(message = AccountRules.PASSWORD_MESSAGE)
	@Pattern(regexp = AccountRules.PASSWORD_REGEX, message = AccountRules.PASSWORD_MESSAGE)
	private String newPassword;

	private String confirmPassword;
}
