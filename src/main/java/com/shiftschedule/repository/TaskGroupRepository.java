package com.shiftschedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.TaskGroup;

public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {

	List<TaskGroup> findAllByOrderBySortOrderAscIdAsc();

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);
}
