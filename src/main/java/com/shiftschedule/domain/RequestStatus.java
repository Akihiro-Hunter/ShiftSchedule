package com.shiftschedule.domain;

/** 次回勤務表作成でユーザーが選ぶ各日の希望 */
public enum RequestStatus {
	AVAILABLE("出勤可能"),
	OFF("休み"),
	UNDECIDED("未定");

	private final String label;

	RequestStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
