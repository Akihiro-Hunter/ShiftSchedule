package com.shiftschedule.domain;

/** 勤務表の期間（16日〜翌15日）を二つに分けた半月。確定は半月ごとに行う */
public enum Half {

	/** 前半（16日〜月末） */
	FIRST("前半"),
	/** 後半（翌月1日〜15日） */
	SECOND("後半");

	private final String label;

	Half(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
