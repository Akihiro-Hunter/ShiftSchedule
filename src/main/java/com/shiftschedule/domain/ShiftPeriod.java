package com.shiftschedule.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 期間ごとの朝アシシフトの確定状態 */
@Entity
@Table(name = "shift_periods")
public class ShiftPeriod {

	@Id
	@Column(name = "period_start")
	private LocalDate periodStart;

	@Column(nullable = false)
	private boolean confirmed;

	private LocalDateTime confirmedAt;

	protected ShiftPeriod() {
	}

	public ShiftPeriod(LocalDate periodStart) {
		this.periodStart = periodStart;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public boolean isConfirmed() {
		return confirmed;
	}

	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}

	public LocalDateTime getConfirmedAt() {
		return confirmedAt;
	}

	public void setConfirmedAt(LocalDateTime confirmedAt) {
		this.confirmedAt = confirmedAt;
	}
}
