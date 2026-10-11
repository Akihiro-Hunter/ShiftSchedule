package com.shiftschedule.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 可能作業（定義順が画面の表示順） */
public enum WorkTask {
	DRY(TaskGroup.WORK, "ドライ"),
	COOL(TaskGroup.WORK, "クール"),
	NEKOPOS(TaskGroup.WORK, "ネコポス"),
	LIFT(TaskGroup.WORK, "リフト"),

	TRUCK_422(TaskGroup.TRUCK, "422"),
	TRUCK_425(TaskGroup.TRUCK, "425"),
	TRUCK_403(TaskGroup.TRUCK, "403"),
	TRUCK_431(TaskGroup.TRUCK, "431"),
	TRUCK_4(TaskGroup.TRUCK, "④"),
	TRUCK_5(TaskGroup.TRUCK, "⑤"),
	TRUCK_6(TaskGroup.TRUCK, "⑥"),
	TRUCK_417(TaskGroup.TRUCK, "417"),
	TRUCK_429(TaskGroup.TRUCK, "429"),
	TRUCK_413(TaskGroup.TRUCK, "413"),
	TRUCK_420(TaskGroup.TRUCK, "420"),
	TRUCK_408(TaskGroup.TRUCK, "408");

	private final TaskGroup group;
	private final String label;

	WorkTask(TaskGroup group, String label) {
		this.group = group;
		this.label = label;
	}

	public TaskGroup getGroup() {
		return group;
	}

	public String getLabel() {
		return label;
	}

	/** 区分ごとの可能作業（画面のチェック欄用。区分・作業とも定義順） */
	public static Map<TaskGroup, List<WorkTask>> byGroup() {
		Map<TaskGroup, List<WorkTask>> map = new LinkedHashMap<>();
		for (TaskGroup group : TaskGroup.values()) {
			map.put(group, new ArrayList<>());
		}
		for (WorkTask task : values()) {
			map.get(task.group).add(task);
		}
		return map;
	}
}
