package com.shiftschedule.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.ShiftPeriod;

public interface ShiftPeriodRepository extends JpaRepository<ShiftPeriod, LocalDate> {
}
