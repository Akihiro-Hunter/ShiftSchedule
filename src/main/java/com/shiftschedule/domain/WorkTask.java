package com.shiftschedule.domain;

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
import lombok.Getter;
import lombok.Setter;

/**
 * 可能作業（例：ドライ、リフト、408）。管理者が編集する。
 * シフトを決める際に参照される（例：リフトができる人を毎日1人以上など）。
 */
@Entity
@Table(name = "work_tasks", uniqueConstraints = @UniqueConstraint(columnNames = { "group_id", "name" }))
@Getter
@Setter
public class WorkTask {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "group_id", nullable = false)
	private TaskGroup group;

	@Column(nullable = false, length = 30)
	private String name;

	/** 大項目内の表示順（小さいほど先） */
	@Column(nullable = false)
	private int sortOrder;

	/** 画面表示用 */
	public String getLabel() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof WorkTask other && id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}
}
