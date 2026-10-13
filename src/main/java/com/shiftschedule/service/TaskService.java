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

import com.shiftschedule.domain.TaskGroup;
import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.repository.TaskGroupRepository;
import com.shiftschedule.repository.WorkTaskRepository;

/** 可能作業（大項目・作業名）の取得と編集 */
@Service
@Transactional
public class TaskService {

	public static final int NAME_MAX = 30;

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

	public void renameGroup(Long id, String rawName) {
		String name = normalize(rawName, "大項目名");
		if (groups.existsByNameAndIdNot(name, id)) {
			throw new IllegalArgumentException("大項目「" + name + "」は既に登録されています");
		}
		groups.findById(id).orElseThrow().setName(name);
	}

	/** 大項目と、その下の作業をすべて削除する（ユーザーの可能作業からも外れる） */
	public String deleteGroup(Long id) {
		TaskGroup group = groups.findById(id).orElseThrow();
		for (WorkTask task : tasks.findByGroup(group)) {
			tasks.detachFromAccounts(task.getId());
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

	public void renameTask(Long id, String rawName) {
		WorkTask task = tasks.findById(id).orElseThrow();
		String name = normalize(rawName, "作業名");
		if (tasks.existsByGroupAndNameAndIdNot(task.getGroup(), name, id)) {
			throw new IllegalArgumentException("「" + task.getGroup().getName() + "」に「" + name + "」は既に登録されています");
		}
		task.setName(name);
	}

	/** 作業を削除する（ユーザーの可能作業からも外れる） */
	public String deleteTask(Long id) {
		WorkTask task = tasks.findById(id).orElseThrow();
		tasks.detachFromAccounts(id);
		tasks.deleteById(id);
		return task.getName();
	}

	/** 画面から届いた可能作業の ID を、存在する作業だけの集合にする */
	@Transactional(readOnly = true)
	public Set<WorkTask> resolve(Collection<Long> ids) {
		return Set.copyOf(findAllById(ids));
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
