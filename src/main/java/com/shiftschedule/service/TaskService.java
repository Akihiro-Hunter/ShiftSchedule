package com.shiftschedule.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.RequirementKind;
import com.shiftschedule.domain.TaskGroup;
import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.repository.TaskGroupRepository;
import com.shiftschedule.repository.WorkTaskRepository;

/** 可能作業（大項目・作業名）の取得と編集 */
@Service
@Transactional
public class TaskService {

	public static final int NAME_MAX = 30;
	public static final int REQUIRED_MAX = 30;

	private final TaskGroupRepository groups;
	private final WorkTaskRepository tasks;

	public TaskService(TaskGroupRepository groups, WorkTaskRepository tasks) {
		this.groups = groups;
		this.tasks = tasks;
	}

	/** 大項目ごとの可能作業（大項目・作業とも表示順。作業のない大項目も含む） */
	@Transactional(readOnly = true)
	public Map<TaskGroup, List<WorkTask>> byGroup() {
		Map<TaskGroup, List<WorkTask>> map = new LinkedHashMap<>();
		for (TaskGroup group : groups.findAllByOrderBySortOrderAscIdAsc()) {
			map.put(group, new ArrayList<>());
		}
		for (WorkTask task : tasks.findAllByOrderByGroup_SortOrderAscGroup_IdAscSortOrderAscIdAsc()) {
			map.get(task.getGroup()).add(task);
		}
		return map;
	}

	@Transactional(readOnly = true)
	public List<WorkTask> findAllById(Collection<Long> ids) {
		return ids == null || ids.isEmpty() ? List.of() : tasks.findAllById(ids);
	}

	/** 作業ごとに、できるユーザーの人数 */
	@Transactional(readOnly = true)
	public Map<Long, Long> userCounts() {
		return tasks.findAll().stream().collect(Collectors.toMap(WorkTask::getId, t -> tasks.countUsers(t.getId())));
	}

	public TaskGroup addGroup(String rawName) {
		String name = normalize(rawName, "大項目名");
		if (groups.existsByName(name)) {
			throw new IllegalArgumentException("大項目「" + name + "」は既に登録されています");
		}
		TaskGroup group = new TaskGroup();
		group.setName(name);
		group.setSortOrder(groups.findAll().stream().mapToInt(TaskGroup::getSortOrder).max().orElse(0) + 1);
		return groups.save(group);
	}

	/**
	 * 大項目と作業名をまとめて追加する。大項目が未登録なら新規に作り、作業名が空なら大項目だけを追加する。
	 */
	public String addEntry(String rawGroup, String rawTask) {
		String groupName = normalize(rawGroup, "大項目名");
		boolean hasTask = rawTask != null && !rawTask.isBlank();
		TaskGroup group = groups.findAll().stream().filter(g -> g.getName().equals(groupName)).findFirst().orElse(null);
		if (group == null) {
			group = addGroup(groupName);
		} else if (!hasTask) {
			throw new IllegalArgumentException("大項目「" + groupName + "」は既に登録されています");
		}
		if (!hasTask) {
			return "大項目「" + groupName + "」を追加しました";
		}
		return "「" + groupName + "」に作業「" + addTask(group.getId(), rawTask).getName() + "」を追加しました";
	}

	/**
	 * 画面で編集された大項目名・作業名をまとめて保存する（名前の入れ替えも可）。
	 * 空欄にされた大項目・作業は削除する（大項目を削除すると、その下の作業もすべて削除）。
	 * 名前が不正・重複なら例外を投げ、全体を保存しない。
	 */
	public void saveNames(Map<Long, String> groupNames, Map<Long, String> taskNames, Map<Long, String> kinds,
			Map<Long, String> counts) {
		groupNames.forEach((id, name) -> {
			if (name == null || name.isBlank()) {
				groups.findById(id).ifPresent(g -> deleteGroup(id));
			}
		});
		taskNames.forEach((id, name) -> {
			if ((name == null || name.isBlank()) && tasks.existsById(id)) {
				deleteTask(id);
			}
		});
		Set<String> seenGroups = new java.util.HashSet<>();
		for (TaskGroup group : groups.findAll()) {
			String raw = groupNames.get(group.getId());
			if (raw != null) {
				group.setName(normalize(raw, "大項目名"));
			}
			if (!seenGroups.add(group.getName())) {
				throw new IllegalArgumentException("大項目「" + group.getName() + "」が重複しています");
			}
		}
		Set<String> seenTasks = new java.util.HashSet<>();
		for (WorkTask task : tasks.findAll()) {
			String raw = taskNames.get(task.getId());
			if (raw != null) {
				task.setName(normalize(raw, "作業名"));
			}
			applyRequirement(task, kinds.get(task.getId()), counts.get(task.getId()));
			if (!seenTasks.add(task.getGroup().getId() + "/" + task.getName())) {
				throw new IllegalArgumentException("「" + task.getGroup().getName() + "」に「" + task.getName() + "」が重複しています");
			}
		}
	}

	/** 大項目と、その下の作業をすべて削除する（ユーザーの可能作業からも外れる） */
	public String deleteGroup(Long id) {
		TaskGroup group = groups.findById(id).orElseThrow();
		for (WorkTask task : tasks.findByGroup(group)) {
			tasks.detachFromAccounts(task.getId());
			tasks.deleteAssignments(task.getId());
			tasks.deleteById(task.getId());
		}
		groups.delete(group);
		return group.getName();
	}

	public WorkTask addTask(Long groupId, String rawName) {
		TaskGroup group = groups.findById(groupId).orElseThrow();
		String name = normalize(rawName, "作業名");
		if (tasks.existsByGroupAndName(group, name)) {
			throw new IllegalArgumentException("「" + group.getName() + "」に「" + name + "」は既に登録されています");
		}
		WorkTask task = new WorkTask();
		task.setGroup(group);
		task.setName(name);
		task.setSortOrder(tasks.findByGroup(group).stream().mapToInt(WorkTask::getSortOrder).max().orElse(0) + 1);
		return tasks.save(task);
	}

	/** 作業を削除する（ユーザーの可能作業からも外れる） */
	public String deleteTask(Long id) {
		WorkTask task = tasks.findById(id).orElseThrow();
		tasks.detachFromAccounts(id);
		tasks.deleteAssignments(id);
		tasks.deleteById(id);
		return task.getName();
	}

	/** 画面から届いた可能作業の ID を、存在する作業だけの集合にする */
	@Transactional(readOnly = true)
	public Set<WorkTask> resolve(Collection<Long> ids) {
		return Set.copyOf(findAllById(ids));
	}

	/** 必要人数の種別と人数を反映する（未指定の項目は変えない） */
	private static void applyRequirement(WorkTask task, String kind, String count) {
		if (kind != null && !kind.isBlank()) {
			try {
				task.setRequiredKind(RequirementKind.valueOf(kind));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("必要人数の種別が正しくありません");
			}
		}
		if (count != null && !count.isBlank()) {
			int n;
			try {
				n = Integer.parseInt(count.strip());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("必要人数は数字で指定してください");
			}
			if (n < 0 || n > REQUIRED_MAX) {
				throw new IllegalArgumentException("必要人数は0〜" + REQUIRED_MAX + "人で指定してください");
			}
			task.setRequiredCount(n);
		}
	}

	private static String normalize(String raw, String label) {
		String name = raw == null ? "" : raw.strip();
		if (name.isEmpty()) {
			throw new IllegalArgumentException(label + "を入力してください");
		}
		if (name.length() > NAME_MAX) {
			throw new IllegalArgumentException(label + "は" + NAME_MAX + "文字以内で入力してください");
		}
		return name;
	}
}
