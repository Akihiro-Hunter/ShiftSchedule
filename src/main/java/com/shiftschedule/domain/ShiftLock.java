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
import lombok.NoArgsConstructor;

/** ユーザー勤務表管理で鍵をかけたユーザー（期間ごと）。鍵がかかっている間、その人の勤怠は変更しない */
@Entity
@Table(name = "t_lock", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "period_start" }))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftLock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	public ShiftLock(Account account, LocalDate periodStart) {
		this.account = account;
		this.periodStart = periodStart;
	}
}
