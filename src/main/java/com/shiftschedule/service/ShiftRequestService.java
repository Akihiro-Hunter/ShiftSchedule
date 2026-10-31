package com.shiftschedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;
import com.shiftschedule.domain.Role;
import com.shiftschedule.domain.ShiftRequest;
import com.shiftschedule.domain.ShiftRequestNote;
import com.shiftschedule.repository.AccountRepository;
import com.shiftschedule.repository.ShiftRequestNoteRepository;
import com.shiftschedule.repository.ShiftRequestRepository;

/** 次回勤務表作成（ユーザーの希望入力） */
@Service
@Transactional
public class ShiftRequestService {

	private final AccountRepository accounts;
	private final ShiftRequestRepository requests;
	private final ShiftRequestNoteRepository notes;

	public ShiftRequestService(AccountRepository accounts, ShiftRequestRepository requests,
			ShiftRequestNoteRepository notes) {
		this.accounts = accounts;
		this.requests = requests;
		this.notes = notes;
	}

	/** 期間内の各日の希望（未入力の日は含まない） */
	@Transactional(readOnly = true)
	public Map<LocalDate, RequestStatus> findStatuses(Long accountId, Period period) {
		Map<LocalDate, RequestStatus> result = new HashMap<>();
		for (ShiftRequest request : requests.findByAccount_IdAndWorkDateBetween(accountId, period.start(),
				period.end())) {
			result.put(request.getWorkDate(), request.getStatus());
		}
		return result;
	}

	@Transactional(readOnly = true)
	public Optional<ShiftRequestNote> findNote(Long accountId, Period period) {
		return notes.findByAccount_IdAndPeriodStart(accountId, period.start());
	}

	/**
	 * 希望と備考を保存する（途中保存可）。
	 *
	 * @param statuses 日付ごとの希望。含まれない日は変更しない
	 */
	public void save(Long accountId, Period period, Map<LocalDate, RequestStatus> statuses, String note) {
		Account account = accounts.findById(accountId).orElseThrow();
		Map<LocalDate, ShiftRequest> existing = new HashMap<>();
		for (ShiftRequest request : requests.findByAccount_IdAndWorkDateBetween(accountId, period.start(),
				period.end())) {
			existing.put(request.getWorkDate(), request);
		}
		statuses.forEach((date, status) -> {
			if (!period.contains(date)) {
				return;
			}
			ShiftRequest request = existing.get(date);
			if (request == null) {
				requests.save(new ShiftRequest(account, date, status));
			} else {
				request.setStatus(status);
			}
		});

		ShiftRequestNote requestNote = notes.findByAccount_IdAndPeriodStart(accountId, period.start())
				.orElseGet(() -> new ShiftRequestNote(account, period.start()));
		requestNote.setNote(note == null ? null : truncate(note.strip(), ShiftRequestNote.NOTE_MAX_LENGTH));
		requestNote.setUpdatedAt(LocalDateTime.now(Period.ZONE));
		notes.save(requestNote);
	}

	/** 期間内に「休み」を希望している日（全員分、アカウントIDごと） */
	@Transactional(readOnly = true)
	public Map<Long, Set<LocalDate>> findOffDays(Period period) {
		Map<Long, Set<LocalDate>> result = new HashMap<>();
		for (ShiftRequest request : requests.findByWorkDateBetween(period.start(), period.end())) {
			if (request.getStatus() == RequestStatus.OFF) {
				result.computeIfAbsent(request.getAccount().getId(), k -> new HashSet<>()).add(request.getWorkDate());
			}
		}
		return result;
	}

	/**
	 * 管理者が口頭などで聞いた休み希望を、全員分まとめて保存する。
	 * 休みから外した日は「出勤可能」に戻す（「出勤可能」「未定」の日は、休みにしない限り変更しない）。
	 *
	 * @param offDays アカウントIDごとの休み希望日。アルバイト・パート以外と期間外の日は無視する
	 */
	public void saveOffDays(Period period, Map<Long, Set<LocalDate>> offDays) {
		Map<Long, Account> staff = new HashMap<>();
		for (Account account : accounts.findByRoleIn(Role.STAFF)) {
			staff.put(account.getId(), account);
		}
		Map<Long, Map<LocalDate, ShiftRequest>> existing = new HashMap<>();
		for (ShiftRequest request : requests.findByWorkDateBetween(period.start(), period.end())) {
			existing.computeIfAbsent(request.getAccount().getId(), k -> new HashMap<>()).put(request.getWorkDate(),
					request);
		}

		for (Account account : staff.values()) {
			Set<LocalDate> off = offDays.getOrDefault(account.getId(), Set.of());
			Map<LocalDate, ShiftRequest> current = existing.getOrDefault(account.getId(), Map.of());
			for (LocalDate date : period.dates()) {
				ShiftRequest request = current.get(date);
				if (off.contains(date)) {
					if (request == null) {
						requests.save(new ShiftRequest(account, date, RequestStatus.OFF));
					} else {
						request.setStatus(RequestStatus.OFF);
					}
				} else if (request != null && request.getStatus() == RequestStatus.OFF) {
					request.setStatus(RequestStatus.AVAILABLE);
				}
			}
		}
	}

	static String truncate(String value, int max) {
		return value.length() <= max ? value : value.substring(0, max);
	}
}
