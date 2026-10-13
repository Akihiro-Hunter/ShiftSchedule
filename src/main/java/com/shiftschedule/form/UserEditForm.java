package com.shiftschedule.form;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Priority;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** ユーザー情報管理（編集）の入力フォーム。優先度・残年次有給休暇数・可能作業のみ */
@Data
public class UserEditForm {

	private Long id;

	/** 表示用（編集不可） */
	private String userNumber;

	/** 表示用（編集不可） */
	private String name;

	@NotNull(message = "優先度を選んでください")
	private Priority priority = Priority.MEDIUM;

	@NotNull(message = "残年次有給休暇数を入力してください")
	@DecimalMin(value = "0", message = "残年次有給休暇数は0以上で入力してください")
	@DecimalMax(value = "99.5", message = "残年次有給休暇数は99.5以下で入力してください")
	@Digits(integer = 2, fraction = 1, message = "残年次有給休暇数は小数第1位までで入力してください")
	private BigDecimal paidLeaveDays = BigDecimal.ZERO;

	/** 可能作業の ID */
	private Set<Long> taskIds = new HashSet<>();

	public static UserEditForm from(Account account) {
		UserEditForm form = new UserEditForm();
		form.id = account.getId();
		form.userNumber = account.getUserNumber();
		form.name = account.getName();
		form.priority = account.getPriority();
		form.paidLeaveDays = account.getPaidLeaveDays();
		account.getTasks().forEach(t -> form.taskIds.add(t.getId()));
		return form;
	}

	public void applyTo(Account account) {
		account.setPriority(priority);
		account.setPaidLeaveDays(paidLeaveDays);
	}

	/** チェックが1つもないと null が届くため空集合にする */
	public void setTaskIds(Set<Long> taskIds) {
		this.taskIds = taskIds == null ? new HashSet<>() : taskIds;
	}
}
