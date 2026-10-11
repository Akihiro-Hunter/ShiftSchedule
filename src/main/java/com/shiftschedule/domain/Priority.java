package com.shiftschedule.domain;

/** シフトを組む際の優先度の目安（管理者のみが閲覧・編集できる） */
public enum Priority {
	HIGH("大"),
	MEDIUM("中"),
	LOW("小");

	private final String label;

	Priority(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
