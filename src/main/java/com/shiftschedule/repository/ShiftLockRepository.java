package com.shiftschedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.ShiftLock;

public interface ShiftLockRepository extends JpaRepository<ShiftLock, Long> {

	List<ShiftLock> findByPeriodStart(LocalDate periodStart);

	void deleteByAccount_Id(Long accountId);
}
