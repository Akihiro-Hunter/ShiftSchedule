package com.shiftschedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** 可能作業の大項目（例：作業、トラック積）。管理者が編集する */
@Entity
@Table(name = "m_task_group")
@Getter
@Setter
public class TaskGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String name;

	/** 表示順（小さいほど先） */
	@Column(nullable = false)
	private int sortOrder;

	/** 画面表示用（旧 enum 時代の getLabel と同じ呼び名） */
	public String getLabel() {
		return name;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof TaskGroup other && id != null && id.equals(other.id);
	}

	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}
}
