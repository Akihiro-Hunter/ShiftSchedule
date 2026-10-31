package com.shiftschedule.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 期間ごとの朝アシシフトの確定状態 */
@Entity
@Table(name = "t_period")
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftPeriod {

	@Id
	@Column(name = "period_start")
	private LocalDate periodStart;

	@Column(nullable = false)
	private boolean confirmed;

	private LocalDateTime confirmedAt;

	public ShiftPeriod(LocalDate periodStart) {
		this.periodStart = periodStart;
	}
}
