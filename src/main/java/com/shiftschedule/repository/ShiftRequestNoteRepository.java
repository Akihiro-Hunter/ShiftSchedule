package com.shiftschedule.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.ShiftRequestNote;

public interface ShiftRequestNoteRepository extends JpaRepository<ShiftRequestNote, Long> {

	List<ShiftRequestNote> findByPeriodStart(LocalDate periodStart);

	Optional<ShiftRequestNote> findByAccount_IdAndPeriodStart(Long accountId, LocalDate periodStart);

	void deleteByAccount_Id(Long accountId);
}
