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

/**
 * 管理者が朝アシシフトに入力した1日分の内容。
 * ユーザーが「休み」にした日は「公／希」で固定されるため、このデータは使わない。
 */
@Entity
@Table(name = "shift_entries", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "work_date" }))
public class ShiftEntry {

	public static final int NOTE_MAX_LENGTH = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(name = "work_date", nullable = false)
	private LocalDate workDate;

	/** 上段：true なら「公」（休み）、false なら空白（出勤） */
	@Column(nullable = false)
	private boolean dayOff;

	/** 下段：自由入力 */
	@Column(length = NOTE_MAX_LENGTH)
	private String note;

	protected ShiftEntry() {
	}

	public ShiftEntry(Account account, LocalDate workDate) {
		this.account = account;
		this.workDate = workDate;
	}

	public Long getId() {
		return id;
	}

	public Account getAccount() {
		return account;
	}

	public LocalDate getWorkDate() {
		return workDate;
	}

	public boolean isDayOff() {
		return dayOff;
	}

	public void setDayOff(boolean dayOff) {
		this.dayOff = dayOff;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}
}
