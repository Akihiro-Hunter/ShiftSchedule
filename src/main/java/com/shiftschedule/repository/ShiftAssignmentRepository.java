package com.shiftschedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.ShiftAssignment;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

	List<ShiftAssignment> findByWorkDateBetween(LocalDate from, LocalDate to);

	void deleteByAccount_Id(Long accountId);

	void deleteByTask_Id(Long taskId);
}
