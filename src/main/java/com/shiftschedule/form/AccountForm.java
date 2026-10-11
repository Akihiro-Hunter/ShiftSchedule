package com.shiftschedule.form;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Priority;
import com.shiftschedule.domain.Role;
import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.service.AccountRules;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** ユーザー情報管理の入力フォーム */
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

	@NotNull(message = "残年次有給休暇数を入力してください")
	@DecimalMin(value = "0", message = "残年次有給休暇数は0以上で入力してください")
	@DecimalMax(value = "99.5", message = "残年次有給休暇数は99.5以下で入力してください")
	@Digits(integer = 2, fraction = 1, message = "残年次有給休暇数は小数第1位までで入力してください")
	private BigDecimal paidLeaveDays = BigDecimal.ZERO;

	@NotNull(message = "役職を選んでください")
	private Role role = Role.USER;

	@NotNull(message = "優先度を選んでください")
	private Priority priority = Priority.MEDIUM;

	private Set<WorkTask> tasks = new HashSet<>();

	/** 新規作成時は必須、編集時は入力した場合のみ変更 */
	private String password;

	public static AccountForm from(Account account) {
		AccountForm form = new AccountForm();
		form.id = account.getId();
		form.userNumber = account.getUserNumber();
		form.name = account.getName();
		form.birthDate = account.getBirthDate();
		form.hireDate = account.getHireDate();
		form.paidLeaveDays = account.getPaidLeaveDays();
		form.role = account.getRole();
		form.priority = account.getPriority();
		form.tasks = new HashSet<>(account.getTasks());
		return form;
	}

	public void applyTo(Account account) {
		account.setUserNumber(userNumber);
		account.setName(name.strip());
		account.setBirthDate(birthDate);
		account.setHireDate(hireDate);
		account.setPaidLeaveDays(paidLeaveDays);
		account.setRole(role);
		account.setPriority(priority);
		account.setTasks(tasks);
	}

	public boolean isNew() {
		return id == null;
	}

	/** チェックが1つもないと null が届くため空集合にする */
	public void setTasks(Set<WorkTask> tasks) {
		this.tasks = tasks == null ? new HashSet<>() : tasks;
	}
}
