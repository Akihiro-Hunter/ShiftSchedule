package com.shiftschedule.domain;

/** 作業の必要人数の種別 */
public enum RequirementKind {

	/** 必須（足りない日は「不足」として報告する） */
	REQUIRED("必須"),
	/** できれば（足りなくても仕方ないが、満たしていると良い） */
	PREFERRED("できれば");

	private final String label;

	RequirementKind(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
