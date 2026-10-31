package com.shiftschedule.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.repository.AccountRepository;
import com.shiftschedule.repository.ShiftAssignmentRepository;
import com.shiftschedule.repository.ShiftEntryRepository;
import com.shiftschedule.repository.ShiftLockRepository;
import com.shiftschedule.repository.ShiftRequestNoteRepository;
import com.shiftschedule.repository.ShiftRequestRepository;

@Service
@Transactional
public class AccountService {

	private final AccountRepository accounts;
	private final ShiftRequestRepository requests;
	private final ShiftRequestNoteRepository requestNotes;
	private final ShiftEntryRepository entries;
	private final ShiftLockRepository locks;
	private final ShiftAssignmentRepository assignments;
	private final PasswordEncoder encoder;

	public AccountService(AccountRepository accounts, ShiftRequestRepository requests,
			ShiftRequestNoteRepository requestNotes, ShiftEntryRepository entries, ShiftLockRepository locks,
			ShiftAssignmentRepository assignments, PasswordEncoder encoder) {
		this.accounts = accounts;
		this.requests = requests;
		this.requestNotes = requestNotes;
		this.entries = entries;
		this.locks = locks;
		this.assignments = assignments;
		this.encoder = encoder;
	}

	@Transactional(readOnly = true)
	public List<Account> findAll() {
		return accounts.findAllByOrderByUserNumber();
	}

	@Transactional(readOnly = true)
	public Optional<Account> findById(Long id) {
		return accounts.findById(id);
	}

	/** ユーザー番号が他のアカウントで使われているか */
	@Transactional(readOnly = true)
	public boolean isUserNumberTaken(String userNumber, Long exceptId) {
		return accounts.findByUserNumber(userNumber)
				.filter(a -> !a.getId().equals(exceptId))
				.isPresent();
	}

	/**
	 * アカウントを保存する。
	 *
	 * @param rawPassword 新しいパスワード（空なら変更しない）
	 */
	public Account save(Account account, String rawPassword) {
		if (rawPassword != null && !rawPassword.isEmpty()) {
			account.setPassword(encoder.encode(rawPassword));
		}
		return accounts.save(account);
	}

	/** アカウントと、そのアカウントの勤務表データを削除する */
	public void delete(Long id) {
		requests.deleteByAccount_Id(id);
		requestNotes.deleteByAccount_Id(id);
		entries.deleteByAccount_Id(id);
		locks.deleteByAccount_Id(id);
		assignments.deleteByAccount_Id(id);
		accounts.deleteById(id);
	}

	public void updateTasks(Long id, Set<WorkTask> tasks) {		accounts.findById(id).orElseThrow().setTasks(tasks);
	}

	/**
	 * パスワードを変更する。
	 *
	 * @return 現在のパスワードが正しく、変更できた場合 true
	 */
	public boolean changePassword(Long id, String currentPassword, String newPassword) {
		Account account = accounts.findById(id).orElseThrow();
		if (!encoder.matches(currentPassword, account.getPassword())) {
			return false;
		}
		account.setPassword(encoder.encode(newPassword));
		return true;
	}
}
