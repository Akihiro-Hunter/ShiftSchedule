package com.shiftschedule.domain;

/** アカウントの権限 */
public enum Role {
	MANAGER("管理者"),
	USER("アルバイト");

	private final String label;

	Role(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
