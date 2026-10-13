package com.shiftschedule.form;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Role;
import com.shiftschedule.service.AccountRules;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** ユーザー登録の入力フォーム */
@Data
public class AccountForm {

	private Long id;

	@NotNull(message = AccountRules.USER_NUMBER_MESSAGE)
	@Pattern(regexp = AccountRules.USER_NUMBER_REGEX, message = AccountRules.USER_NUMBER_MESSAGE)
	private String userNumber;

	@NotBlank(message = "氏名を入力してください")
	@Size(max = 50, message = "氏名は50文字以内で入力してください")
	private String name;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate birthDate;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate hireDate;

	@NotNull(message = "契約勤務日数を選んでください")
	@Min(value = 3, message = "契約勤務日数は週3〜6日で選んでください")
	@Max(value = 6, message = "契約勤務日数は週3〜6日で選んでください")
	private Integer contractDays = 5;

	@NotNull(message = "勤務開始時刻を入力してください")
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime startTime = LocalTime.of(5, 0);

	@NotNull(message = "勤務終了時刻を入力してください")
	@DateTimeFormat(pattern = "HH:mm")
	private LocalTime endTime = LocalTime.of(8, 0);

	@NotNull(message = "役職を選んでください")
	private Role role = Role.USER;

	/** 新規登録時は必須、編集時は入力した場合のみ変更 */
	private String password;

	public static AccountForm from(Account account) {
		AccountForm form = new AccountForm();
		form.id = account.getId();
		form.userNumber = account.getUserNumber();
		form.name = account.getName();
		form.birthDate = account.getBirthDate();
		form.hireDate = account.getHireDate();
		form.contractDays = account.getContractDays();
		form.startTime = account.getStartTime();
		form.endTime = account.getEndTime();
		form.role = account.getRole();
		return form;
	}

	public void applyTo(Account account) {
		account.setUserNumber(userNumber);
		account.setName(name.strip());
		account.setBirthDate(birthDate);
		account.setHireDate(hireDate);
		account.setContractDays(contractDays);
		account.setStartTime(startTime);
		account.setEndTime(endTime);
		account.setRole(role);
	}

	public boolean isNew() {
		return id == null;
	}
}
