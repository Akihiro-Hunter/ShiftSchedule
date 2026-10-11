package com.shiftschedule.domain;

/** 可能作業 */
public enum WorkTask {
	COOL("クール"),
	NEKOPOS("ネコポス"),
	NAEBO("苗穂"),
	KANJO("環状"),
	KOSEI("光星"),
	LIFT("リフト");

	private final String label;

	WorkTask(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
