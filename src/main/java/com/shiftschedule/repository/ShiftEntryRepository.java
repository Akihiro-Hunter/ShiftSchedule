package com.shiftschedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.ShiftEntry;

public interface ShiftEntryRepository extends JpaRepository<ShiftEntry, Long> {

	List<ShiftEntry> findByWorkDateBetween(LocalDate from, LocalDate to);

	void deleteByAccount_Id(Long accountId);
}
