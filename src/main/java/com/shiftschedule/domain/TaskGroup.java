package com.shiftschedule.domain;

/** 可能作業の区分 */
public enum TaskGroup {
	WORK("作業"),
	TRUCK("トラック積");

	private final String label;

	TaskGroup(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
