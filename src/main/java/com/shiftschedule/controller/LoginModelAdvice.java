package com.shiftschedule.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.shiftschedule.security.LoginAccount;

/** 全画面のヘッダーで使うログイン中ユーザー名 */
@ControllerAdvice
public class LoginModelAdvice {

	@ModelAttribute("loginName")
	public String loginName(@AuthenticationPrincipal LoginAccount account) {
		return account == null ? null : account.getName();
	}
}
