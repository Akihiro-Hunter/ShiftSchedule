package com.shiftschedule.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 自動作成した「誰がいつどの作業に入るか」。1人が同じ日に複数の作業（時間帯が違えば）に入れる。
 * 出勤・休みは ShiftEntry、どの作業かはこのデータが持つ。
 */
@Entity
@Table(name = "t_assignment", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "work_date", "task_id" }))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account account;

	@Column(name = "work_date", nullable = false)
	private LocalDate workDate;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "task_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private WorkTask task;

	public ShiftAssignment(Account account, LocalDate workDate, WorkTask task) {
		this.account = account;
		this.workDate = workDate;
		this.task = task;
	}
}
