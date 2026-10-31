package com.shiftschedule.security;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.TaskGroup;
import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.repository.TaskGroupRepository;
import com.shiftschedule.repository.WorkTaskRepository;

/**
 * 可能作業が1件もないときに、初期の大項目・作業を登録する。
 * 以前の enum 版（account_tasks テーブル）が残っていれば、ユーザーごとの可能作業も引き継ぐ。
 */
@Component
@Order(0)
public class InitialTaskRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(InitialTaskRunner.class);

	/** legacyName は旧 enum の名前（account_tasks.task に入っていた値） */
	private record Seed(String legacyName, String group, String label) {
	}

	/** 初期の大項目と作業（旧 enum の定義順） */
	private static final List<Seed> SEEDS = List.of(
			new Seed("DRY", "作業", "ドライ"),
			new Seed("COOL", "作業", "クール"),
			new Seed("NEKOPOS", "作業", "ネコポス"),
			new Seed("LIFT", "作業", "リフト"),
			new Seed("TRUCK_422", "トラック積", "422"),
			new Seed("TRUCK_425", "トラック積", "425"),
			new Seed("TRUCK_403", "トラック積", "403"),
			new Seed("TRUCK_431", "トラック積", "431"),
			new Seed("TRUCK_4", "トラック積", "④"),
			new Seed("TRUCK_5", "トラック積", "⑤"),
			new Seed("TRUCK_6", "トラック積", "⑥"),
			new Seed("TRUCK_417", "トラック積", "417"),
			new Seed("TRUCK_429", "トラック積", "429"),
			new Seed("TRUCK_413", "トラック積", "413"),
			new Seed("TRUCK_420", "トラック積", "420"),
			new Seed("TRUCK_408", "トラック積", "408"));

	private final TaskGroupRepository groups;
	private final WorkTaskRepository tasks;
	private final JdbcTemplate jdbc;

	public InitialTaskRunner(TaskGroupRepository groups, WorkTaskRepository tasks, JdbcTemplate jdbc) {
		this.groups = groups;
		this.tasks = tasks;
		this.jdbc = jdbc;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (groups.count() > 0 || tasks.count() > 0) {
			return;
		}
		Map<String, WorkTask> byLegacyName = new HashMap<>();
		Map<String, TaskGroup> groupByName = new LinkedHashMap<>();
		int taskOrder = 0;
		for (Seed seed : SEEDS) {
			TaskGroup group = groupByName.computeIfAbsent(seed.group(), name -> {
				TaskGroup g = new TaskGroup();
				g.setName(name);
				g.setSortOrder(groupByName.size() + 1);
				return groups.save(g);
			});
			WorkTask task = new WorkTask();
			task.setGroup(group);
			task.setName(seed.label());
			task.setSortOrder(++taskOrder);
			byLegacyName.put(seed.legacyName(), tasks.save(task));
		}
		tasks.flush();

		int migrated = 0;
		if (jdbc.queryForObject("SELECT to_regclass('account_tasks') IS NOT NULL", Boolean.class)) {
			for (Map<String, Object> row : jdbc.queryForList("SELECT account_id, task FROM account_tasks")) {
				WorkTask task = byLegacyName.get((String) row.get("task"));
				if (task != null) {
					jdbc.update("INSERT INTO m_user_possible_work (account_id, task_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
							row.get("account_id"), task.getId());
					migrated++;
				}
			}
		}
		log.info("可能作業の初期データを登録しました（ユーザーの可能作業 {} 件を引き継ぎ）。", migrated);
	}
}
