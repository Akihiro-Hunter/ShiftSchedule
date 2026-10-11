package com.shiftschedule.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.shiftschedule.domain.Account;

/** ログイン中のアカウント情報（Spring Security 用） */
public class LoginAccount implements UserDetails {

	private final Long id;
	private final String userNumber;
	private final String password;
	private final String name;
	private final List<GrantedAuthority> authorities;

	public LoginAccount(Account account) {
		this.id = account.getId();
		this.userNumber = account.getUserNumber();
		this.password = account.getPassword();
		this.name = account.getName();
		this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()));
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	@Override
	public String getUsername() {
		return userNumber;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
