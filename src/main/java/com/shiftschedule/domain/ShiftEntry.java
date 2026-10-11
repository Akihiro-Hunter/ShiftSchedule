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
 * 管理者が朝アシシフトに入力した1日分の内容。
 * ユーザーが「休み」にした日は「公／希」で固定されるため、このデータは使わない。
 */
@Entity
@Table(name = "shift_entries", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "work_date" }))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftEntry {

	public static final int NOTE_MAX_LENGTH = 10;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** LAZY のため toString / equals では読み込まない */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account account;

	@Column(name = "work_date", nullable = false)
	private LocalDate workDate;

	/** 上段：true なら「公」（休み）、false なら空白（出勤） */
	@Column(nullable = false)
	private boolean dayOff;

	/** 下段：自由入力 */
	@Column(length = NOTE_MAX_LENGTH)
	private String note;

	public ShiftEntry(Account account, LocalDate workDate) {
		this.account = account;
		this.workDate = workDate;
	}
}
