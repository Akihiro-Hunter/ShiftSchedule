package com.shiftschedule.service;

import java.util.regex.Pattern;

/** ユーザー番号・パスワードの形式ルール */
public final class AccountRules {

	/** ユーザー番号：半角数字8桁 */
	public static final String USER_NUMBER_REGEX = "\\d{8}";

	/** パスワード：半角英数字8文字以上 */
	public static final String PASSWORD_REGEX = "[A-Za-z0-9]{8,}";

	public static final String USER_NUMBER_MESSAGE = "ユーザー番号は半角数字8桁で入力してください";

	public static final String PASSWORD_MESSAGE = "パスワードは半角英数字8文字以上で入力してください";

	private static final Pattern USER_NUMBER = Pattern.compile(USER_NUMBER_REGEX);
	private static final Pattern PASSWORD = Pattern.compile(PASSWORD_REGEX);

	private AccountRules() {
	}

	public static boolean isValidUserNumber(String value) {
		return value != null && USER_NUMBER.matcher(value).matches();
	}

	public static boolean isValidPassword(String value) {
		return value != null && PASSWORD.matcher(value).matches();
	}
}
