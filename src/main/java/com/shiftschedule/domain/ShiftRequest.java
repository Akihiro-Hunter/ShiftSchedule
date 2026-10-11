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

/** 次回勤務表作成でユーザーが入力した、1日分の希望 */
@Entity
@Table(name = "shift_requests", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "work_date" }))
public class ShiftRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id")
	private Account account;

	@Column(name = "work_date", nullable = false)
	private LocalDate workDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RequestStatus status;

	protected ShiftRequest() {
	}

	public ShiftRequest(Account account, LocalDate workDate, RequestStatus status) {
		this.account = account;
		this.workDate = workDate;
		this.status = status;
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

	public RequestStatus getStatus() {
		return status;
	}

	public void setStatus(RequestStatus status) {
		this.status = status;
	}
}
