package com.shiftschedule.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Role;
import com.shiftschedule.repository.AccountRepository;
import com.shiftschedule.service.AccountRules;

/**
 * 動作確認用のアルバイトを作成する（未設定なら何もしない）。
 * ユーザー番号とパスワードは config/application.properties で指定する。
 * 初期管理者の作成後に実行し、ユーザー番号の衝突を避ける。
 */
@Component
@Order(2)
public class InitialUserRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InitialUserRunner.class);

	private final AccountRepository accounts;
	private final PasswordEncoder encoder;
	private final String userNumber;
	private final String password;
	private final String name;

	public InitialUserRunner(AccountRepository accounts, PasswordEncoder encoder,
			@Value("${app.initial-user.user-number:}") String userNumber,
			@Value("${app.initial-user.password:}") String password,
			@Value("${app.initial-user.name:テストユーザー}") String name) {
		this.accounts = accounts;
		this.encoder = encoder;
		this.userNumber = userNumber;
		this.password = password;
		this.name = name;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (userNumber.isEmpty() || accounts.existsByUserNumber(userNumber)) {
			return;
		}
		if (!AccountRules.isValidUserNumber(userNumber) || !AccountRules.isValidPassword(password)) {
			log.warn("app.initial-user.user-number（半角数字8桁）と "
					+ "app.initial-user.password（半角英数字8文字以上）の形式が正しくありません。");
			return;
		}
		Account user = new Account();
		user.setUserNumber(userNumber);
		user.setPassword(encoder.encode(password));
		user.setName(name);
		user.setRole(Role.USER);
		accounts.save(user);
		log.info("初期アルバイト（ユーザー番号 {}）を作成しました。", userNumber);
	}
}
