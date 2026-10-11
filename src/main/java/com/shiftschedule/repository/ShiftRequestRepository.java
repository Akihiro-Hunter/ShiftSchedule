package com.shiftschedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.ShiftRequest;

public interface ShiftRequestRepository extends JpaRepository<ShiftRequest, Long> {

	List<ShiftRequest> findByWorkDateBetween(LocalDate from, LocalDate to);

	List<ShiftRequest> findByAccount_IdAndWorkDateBetween(Long accountId, LocalDate from, LocalDate to);

	void deleteByAccount_Id(Long accountId);
}
