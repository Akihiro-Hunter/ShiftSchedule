package com.shiftschedule.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.DailyPlan;

public interface DailyPlanRepository extends JpaRepository<DailyPlan, LocalDate> {

	List<DailyPlan> findByWorkDateBetween(LocalDate from, LocalDate to);
}
