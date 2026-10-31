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
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 次回勤務表作成の備考と最終保存日時（ユーザー・期間ごと） */
@Entity
@Table(name = "t_request_note", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "period_start" }))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftRequestNote {

	public static final int NOTE_MAX_LENGTH = 500;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** LAZY のため toString / equals では読み込まない */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Account account;

	@Column(name = "period_start", nullable = false)
	private LocalDate periodStart;

	@Column(length = NOTE_MAX_LENGTH)
	private String note;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public ShiftRequestNote(Account account, LocalDate periodStart) {
		this.account = account;
		this.periodStart = periodStart;
	}
}
