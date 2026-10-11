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
 * 管理者が1人もいない場合に、初期管理者を作成する。
 * ユーザー番号とパスワードは config/application.properties で指定する。
 */
@Component
@Order(1)
public class InitialManagerRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InitialManagerRunner.class);

	private final AccountRepository accounts;
	private final PasswordEncoder encoder;
	private final String userNumber;
	private final String password;

	public InitialManagerRunner(AccountRepository accounts, PasswordEncoder encoder,
			@Value("${app.initial-manager.user-number:}") String userNumber,
			@Value("${app.initial-manager.password:}") String password) {
		this.accounts = accounts;
		this.encoder = encoder;
		this.userNumber = userNumber;
		this.password = password;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (accounts.existsByRole(Role.MANAGER)) {
			return;
		}
		if (!AccountRules.isValidUserNumber(userNumber) || !AccountRules.isValidPassword(password)) {
			log.warn("管理者が登録されていません。config/application.properties に "
					+ "app.initial-manager.user-number（半角数字8桁）と "
					+ "app.initial-manager.password（半角英数字8文字以上）を設定して再起動してください。");
			return;
		}
		if (accounts.existsByUserNumber(userNumber)) {
			log.warn("初期管理者のユーザー番号 {} は既にアルバイトとして使われています。", userNumber);
			return;
		}
		Account manager = new Account();
		manager.setUserNumber(userNumber);
		manager.setPassword(encoder.encode(password));
		manager.setName("管理者");
		manager.setRole(Role.MANAGER);
		accounts.save(manager);
		log.info("初期管理者（ユーザー番号 {}）を作成しました。", userNumber);
	}
}
