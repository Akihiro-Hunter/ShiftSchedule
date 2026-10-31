package com.shiftschedule.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shiftschedule.domain.TaskGroup;
import com.shiftschedule.domain.WorkTask;

public interface WorkTaskRepository extends JpaRepository<WorkTask, Long> {

	List<WorkTask> findAllByOrderByGroup_SortOrderAscGroup_IdAscSortOrderAscIdAsc();

	List<WorkTask> findByGroup(TaskGroup group);

	boolean existsByGroupAndName(TaskGroup group, String name);

	boolean existsByGroupAndNameAndIdNot(TaskGroup group, String name, Long id);

	/** その作業ができるユーザー数 */
	@Query(value = "SELECT COUNT(*) FROM m_user_possible_work WHERE task_id = :taskId", nativeQuery = true)
	long countUsers(@Param("taskId") Long taskId);

	/** 作業を削除する前に、自動作成した作業の割り当てを消す */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "DELETE FROM t_assignment WHERE task_id = :taskId", nativeQuery = true)
	void deleteAssignments(@Param("taskId") Long taskId);

	/** 作業を削除する前に、ユーザーの可能作業から外す */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "DELETE FROM m_user_possible_work WHERE task_id = :taskId", nativeQuery = true)
	void detachFromAccounts(@Param("taskId") Long taskId);
}
