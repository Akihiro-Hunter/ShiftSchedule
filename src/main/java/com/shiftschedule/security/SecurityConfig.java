package com.shiftschedule.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.shiftschedule.domain.Role;
import com.shiftschedule.repository.AccountRepository;

/**
 * 管理者（/manager）とアルバイト（/user）で入口を分けたログイン設定。
 * それぞれの入口では、その権限を持つアカウントしかログインできない。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	DaoAuthenticationProvider managerAuthenticationProvider(AccountRepository accounts, PasswordEncoder encoder) {
		return authenticationProvider(accounts, encoder, Role.MANAGER);
	}

	@Bean
	DaoAuthenticationProvider userAuthenticationProvider(AccountRepository accounts, PasswordEncoder encoder) {
		return authenticationProvider(accounts, encoder, Role.USER);
	}

	@Bean
	@Order(1)
	SecurityFilterChain managerFilterChain(HttpSecurity http, DaoAuthenticationProvider managerAuthenticationProvider)
			throws Exception {
		return areaFilterChain(http, "/manager", Role.MANAGER, managerAuthenticationProvider);
	}

	@Bean
	@Order(2)
	SecurityFilterChain userFilterChain(HttpSecurity http, DaoAuthenticationProvider userAuthenticationProvider)
			throws Exception {
		return areaFilterChain(http, "/user", Role.USER, userAuthenticationProvider);
	}

	/** /manager・/user 以外（トップページ、CSS など） */
	@Bean
	@Order(3)
	SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/css/**", "/js/**", "/favicon.ico", "/error").permitAll()
				.anyRequest().denyAll());
		return http.build();
	}

	private static DaoAuthenticationProvider authenticationProvider(AccountRepository accounts,
			PasswordEncoder encoder, Role role) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userNumber -> accounts
				.findByUserNumber(userNumber)
				.filter(account -> account.getRole() == role)
				.map(LoginAccount::new)
				.orElseThrow(() -> new UsernameNotFoundException(userNumber)));
		provider.setPasswordEncoder(encoder);
		return provider;
	}

	private static SecurityFilterChain areaFilterChain(HttpSecurity http, String base, Role role,
			DaoAuthenticationProvider provider) throws Exception {
		http.securityMatcher(base + "/**")
				.authenticationManager(new ProviderManager(provider))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(base + "/login").permitAll()
						.anyRequest().hasRole(role.name()))
				.formLogin(form -> form
						.loginPage(base + "/login")
						.usernameParameter("userNumber")
						.passwordParameter("password")
						.defaultSuccessUrl(base + "/menu", true)
						.failureUrl(base + "/login?error"))
				.logout(logout -> logout
						.logoutUrl(base + "/logout")
						.logoutSuccessUrl(base + "/login?logout"))
				// 別の権限でログイン中に入った場合は、この入口のログイン画面へ
				.exceptionHandling(ex -> ex.accessDeniedHandler(
						(request, response, e) -> response.sendRedirect(request.getContextPath() + base + "/login")));
		return http.build();
	}
}
