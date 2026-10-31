package com.shiftschedule.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** 次回勤務表作成でユーザーが入力した、1日分の希望 */
@Entity
@Table(name = "t_request", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "work_date" }))
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShiftRequest {

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RequestStatus status;

	public ShiftRequest(Account account, LocalDate workDate, RequestStatus status) {
		this.account = account;
		this.workDate = workDate;
		this.status = status;
	}
}
