package com.shiftschedule.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

/** 次回勤務表作成の備考と最終保存日時（ユーザー・期間ごと） */
@Entity
@Table(name = "shift_request_notes", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "period_start" }))
public class ShiftRequestNote {

	public static final int NOTE_MAX_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	@Column(length = NOTE_MAX_LENGTH)
	private String note;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected ShiftRequestNote() {
	}

	public ShiftRequestNote(Account account, LocalDate periodStart) {
		this.account = account;
		this.periodStart = periodStart;
	}

	public Long getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
