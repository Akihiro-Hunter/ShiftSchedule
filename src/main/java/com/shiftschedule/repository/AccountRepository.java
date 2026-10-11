package com.shiftschedule.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Role;

public interface AccountRepository extends JpaRepository<Account, Long> {

	Optional<Account> findByUserNumber(String userNumber);

	boolean existsByUserNumber(String userNumber);

	boolean existsByRole(Role role);

	List<Account> findAllByOrderByUserNumber();

	List<Account> findByRoleOrderByUserNumber(Role role);
}
