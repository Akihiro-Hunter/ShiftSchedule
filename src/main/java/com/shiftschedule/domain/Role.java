package com.shiftschedule.domain;

import java.util.List;

/** アカウントの役職 */
public enum Role {
	MANAGER("管理者"),
	USER("アルバイト"),
	PART("パート");

	/** /user からログインし、朝アシシフトに載る役職（アルバイトとパートはできることが同じ） */
	public static final List<Role> STAFF = List.of(USER, PART);

	private final String label;

	Role(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public boolean isStaff() {
		return STAFF.contains(this);
	}
}
