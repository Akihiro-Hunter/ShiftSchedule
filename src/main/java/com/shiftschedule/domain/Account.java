package com.shiftschedule.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;

/** 管理者・アルバイトのアカウントと個人情報 */
@Entity
@Table(name = "m_user")
@Data
public class Account {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** ユーザー番号（半角数字8桁） */
	@Column(name = "user_number", nullable = false, unique = true, length = 8)
	private String userNumber;

	/** パスワード（ハッシュ化して保存） */
	@Column(nullable = false)
	private String password;

	@Column(nullable = false, length = 50)
	private String name;

	private LocalDate hireDate;

	/** 残年次有給休暇数（0.5日単位） */
	@Column(nullable = false, precision = 4, scale = 1)
	private BigDecimal paidLeaveDays = BigDecimal.ZERO;

	/** 契約勤務日数（週あたり、3〜6日） */
	@Column(nullable = false)
	@ColumnDefault("5")
	private Integer contractDays = 5;

	/** 勤務開始時刻（CSV の「開始」になる） */
	@Column(nullable = false)
	@ColumnDefault("'05:00:00'")
	private LocalTime startTime = LocalTime.of(5, 0);

	/** 勤務終了時刻（CSV の「修了」になる） */
	@Column(nullable = false)
	@ColumnDefault("'08:00:00'")
	private LocalTime endTime = LocalTime.of(8, 0);

	/** 役職 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	/** シフトを組む際の優先度の目安（管理者のみ閲覧・編集可。ユーザー側には出さない） */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	@ColumnDefault("'MEDIUM'")
	private Priority priority = Priority.MEDIUM;

	/** 朝アシシフトでの並び順（管理者がドラッグで並び替える。未設定はユーザー番号順で末尾） */
	private Integer sortOrder;

	/** 可能作業 */
	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "m_user_possible_work", joinColumns = @JoinColumn(name = "account_id"),
			inverseJoinColumns = @JoinColumn(name = "task_id"))
	private Set<WorkTask> tasks = new HashSet<>();

	/** Hibernate が管理するコレクションを差し替えないよう、中身を入れ替える */
	public void setTasks(Set<WorkTask> tasks) {
		this.tasks.clear();
		if (tasks != null) {
			this.tasks.addAll(tasks);
		}
	}

	/** 可能作業を区分ごとにまとめた表示（例：作業：ドライ、クール　トラック積：422、④）。なければ空文字 */
	public String getTasksLabel() {
		Map<TaskGroup, List<String>> byGroup = new LinkedHashMap<>();
		tasks.stream()
				.sorted(Comparator.comparingInt((WorkTask t) -> t.getGroup().getSortOrder())
						.thenComparing(t -> t.getGroup().getId())
						.thenComparingInt(WorkTask::getSortOrder)
						.thenComparing(WorkTask::getId))
				.forEach(t -> byGroup.computeIfAbsent(t.getGroup(), g -> new ArrayList<>()).add(t.getLabel()));
		List<String> parts = new ArrayList<>();
		byGroup.forEach((group, labels) -> parts.add(group.getLabel() + "：" + String.join("、", labels)));
		return String.join("　", parts);
	}
}
