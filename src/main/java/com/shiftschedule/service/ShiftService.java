package com.shiftschedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.DailyPlan;
import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;
import com.shiftschedule.domain.Role;
import com.shiftschedule.domain.ShiftEntry;
import com.shiftschedule.domain.ShiftLock;
import com.shiftschedule.domain.ShiftPeriod;
import com.shiftschedule.domain.ShiftRequest;
import com.shiftschedule.domain.ShiftRequestNote;
import com.shiftschedule.repository.AccountRepository;
import com.shiftschedule.repository.DailyPlanRepository;
import com.shiftschedule.repository.ShiftEntryRepository;
import com.shiftschedule.repository.ShiftLockRepository;
import com.shiftschedule.repository.ShiftPeriodRepository;
import com.shiftschedule.repository.ShiftRequestNoteRepository;
import com.shiftschedule.repository.ShiftRequestRepository;

/** 朝アシシフト（管理者のユーザー勤務表管理、ユーザーの勤務表確認） */
@Service
@Transactional
public class ShiftService {

	private final AccountRepository accounts;
	private final ShiftRequestRepository requests;
	private final ShiftRequestNoteRepository requestNotes;
	private final ShiftEntryRepository entries;
	private final ShiftPeriodRepository periods;
	private final DailyPlanRepository plans;
	private final ShiftLockRepository locks;

	public ShiftService(AccountRepository accounts, ShiftRequestRepository requests,
			ShiftRequestNoteRepository requestNotes, ShiftEntryRepository entries, ShiftPeriodRepository periods,
			DailyPlanRepository plans, ShiftLockRepository locks) {
		this.accounts = accounts;
		this.requests = requests;
		this.requestNotes = requestNotes;
		this.entries = entries;
		this.periods = periods;
		this.plans = plans;
		this.locks = locks;
	}

	@Transactional(readOnly = true)
	public boolean isConfirmed(Period period) {
		return periods.findById(period.start()).map(ShiftPeriod::isConfirmed).orElse(false);
	}

	/** 期間の朝アシシフトを組み立てる */
	@Transactional(readOnly = true)
	public ShiftSheet buildSheet(Period period) {
		Map<Long, Map<LocalDate, RequestStatus>> requestMap = new HashMap<>();
		for (ShiftRequest r : requests.findByWorkDateBetween(period.start(), period.end())) {
			requestMap.computeIfAbsent(r.getAccount().getId(), k -> new HashMap<>()).put(r.getWorkDate(), r.getStatus());
		}
		Map<Long, Map<LocalDate, ShiftEntry>> entryMap = entriesByAccount(period);
		Map<Long, ShiftRequestNote> noteMap = new HashMap<>();
		for (ShiftRequestNote n : requestNotes.findByPeriodStart(period.start())) {
			noteMap.put(n.getAccount().getId(), n);
		}

		Set<Long> lockedIds = lockedAccountIds(period);

		List<LocalDate> dates = period.dates();
		List<ShiftSheet.Row> rows = new ArrayList<>();
		for (Account user : staffInOrder()) {
			Map<LocalDate, RequestStatus> userRequests = requestMap.getOrDefault(user.getId(), Map.of());
			Map<LocalDate, ShiftEntry> userEntries = entryMap.getOrDefault(user.getId(), Map.of());
			List<ShiftSheet.Cell> cells = new ArrayList<>();
			for (LocalDate date : dates) {
				RequestStatus request = userRequests.get(date);
				if (request == RequestStatus.OFF) {
					cells.add(new ShiftSheet.Cell(date, request, true, ShiftSheet.REQUESTED_OFF_MARK, true));
				} else {
					ShiftEntry entry = userEntries.get(date);
					boolean dayOff = entry != null && entry.isDayOff();
					String bottom = entry == null || entry.getNote() == null ? "" : entry.getNote();
					cells.add(new ShiftSheet.Cell(date, request, dayOff, bottom, false));
				}
			}
			ShiftRequestNote note = noteMap.get(user.getId());
			rows.add(new ShiftSheet.Row(user.getId(), user.getUserNumber(), user.getName(), user.getTasksLabel(), cells,
					note == null ? null : note.getNote(), note == null ? null : note.getUpdatedAt(),
					lockedIds.contains(user.getId())));
		}

		Map<LocalDate, Integer> planMap = new HashMap<>();
		for (DailyPlan plan : plans.findByWorkDateBetween(period.start(), period.end())) {
			planMap.put(plan.getWorkDate(), plan.getPlannedCount());
		}
		List<Integer> plannedCounts = new ArrayList<>();
		for (LocalDate date : dates) {
			plannedCounts.add(planMap.getOrDefault(date, DailyPlan.defaultCount(date)));
		}

		ShiftPeriod status = periods.findById(period.start()).orElse(null);
		return new ShiftSheet(period, dates, rows, plannedCounts, status != null && status.isConfirmed(),
				status == null ? null : status.getConfirmedAt());
	}

	/**
	 * 管理者の入力を保存する。ユーザーが休みを希望した日は変更しない。
	 *
	 * @param params 画面のフォーム値（off_{キー}=上段が公、note_{キー}=下段の文字）
	 */
	public void saveEntries(Period period, Map<String, String> params) {
		ShiftSheet current = buildSheet(period);
		Map<Long, Map<LocalDate, ShiftEntry>> entryMap = entriesByAccount(period);

		for (ShiftSheet.Row row : current.rows()) {
			// 鍵がかかったままの人は変更しない（画面でも入力できないが、念のためサーバー側でも守る）
			if (row.editLocked() && isLockRequested(params, row.accountId())) {
				continue;
			}
			Map<LocalDate, ShiftEntry> userEntries = entryMap.getOrDefault(row.accountId(), Map.of());
			for (ShiftSheet.Cell cell : row.cells()) {
				ShiftEntry entry = userEntries.get(cell.date());
				if (cell.locked()) {
					// 休み希望の日は「公／希」で固定。以前の入力が残っていれば消す
					if (entry != null) {
						entries.delete(entry);
					}
					continue;
				}
				String key = cell.key(row.accountId());
				boolean dayOff = params.containsKey("off_" + key);
				String note = params.getOrDefault("note_" + key, "").strip();
				note = ShiftRequestService.truncate(note, ShiftEntry.NOTE_MAX_LENGTH);

				if (!dayOff && note.isEmpty()) {
					if (entry != null) {
						entries.delete(entry);
					}
					continue;
				}
				if (entry == null) {
					entry = new ShiftEntry(accounts.getReferenceById(row.accountId()), cell.date());
				}
				entry.setDayOff(dayOff);
				entry.setNote(note.isEmpty() ? null : note);
				entries.save(entry);
			}
		}
	}

	/**
	 * 鍵の状態を保存する。
	 *
	 * @param params 画面のフォーム値（lock_{アカウントID}=true なら鍵をかける）。項目がない人は変更しない
	 */
	public void saveLocks(Period period, Map<String, String> params) {
		Map<Long, ShiftLock> existing = new HashMap<>();
		for (ShiftLock lock : locks.findByPeriodStart(period.start())) {
			existing.put(lock.getAccount().getId(), lock);
		}
		for (Account account : accounts.findByRoleIn(Role.STAFF)) {
			String value = params.get("lock_" + account.getId());
			if (value == null) {
				continue;
			}
			ShiftLock lock = existing.get(account.getId());
			if (Boolean.parseBoolean(value) && lock == null) {
				locks.save(new ShiftLock(account, period.start()));
			} else if (!Boolean.parseBoolean(value) && lock != null) {
				locks.delete(lock);
			}
		}
	}

	private static boolean isLockRequested(Map<String, String> params, Long accountId) {
		String value = params.get("lock_" + accountId);
		// 項目がない（古い画面からの送信など）場合は、鍵がかかっているものとして扱う
		return value == null || Boolean.parseBoolean(value);
	}

	private Set<Long> lockedAccountIds(Period period) {
		Set<Long> ids = new HashSet<>();
		for (ShiftLock lock : locks.findByPeriodStart(period.start())) {
			ids.add(lock.getAccount().getId());
		}
		return ids;
	}

	/**
	 * 日ごとの予定人数を保存する。既定値と同じ日は保存しない（既定値が変わっても追従するように）。
	 *
	 * @param params 画面のフォーム値（plan_{yyyy-MM-dd}=予定人数）。不正な値の日は変更しない
	 */
	public void savePlans(Period period, Map<String, String> params) {
		Map<LocalDate, DailyPlan> existing = new HashMap<>();
		for (DailyPlan plan : plans.findByWorkDateBetween(period.start(), period.end())) {
			existing.put(plan.getWorkDate(), plan);
		}
		for (LocalDate date : period.dates()) {
			Integer count = parseCount(params.get("plan_" + date));
			if (count == null) {
				continue;
			}
			DailyPlan plan = existing.get(date);
			if (count == DailyPlan.defaultCount(date)) {
				if (plan != null) {
					plans.delete(plan);
				}
			} else if (plan == null) {
				plans.save(new DailyPlan(date, count));
			} else {
				plan.setPlannedCount(count);
			}
		}
	}

	private static Integer parseCount(String value) {
		if (value == null) {
			return null;
		}
		try {
			int count = Integer.parseInt(value.strip());
			return count >= 0 && count <= DailyPlan.MAX_COUNT ? count : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 朝アシシフトの並び順を保存する。
	 *
	 * @param accountIds 上から順に並べたアカウントID（アルバイト・パート以外は無視）
	 */
	public void saveOrder(List<Long> accountIds) {
		Map<Long, Account> staff = new HashMap<>();
		for (Account account : accounts.findByRoleIn(Role.STAFF)) {
			staff.put(account.getId(), account);
		}
		int order = 0;
		for (Long id : accountIds) {
			Account account = staff.get(id);
			if (account != null) {
				account.setSortOrder(order++);
			}
		}
	}

	/** 確定（ユーザーに公開）または確定取消 */
	public void setConfirmed(Period period, boolean confirmed) {
		ShiftPeriod status = periods.findById(period.start()).orElseGet(() -> new ShiftPeriod(period.start()));
		status.setConfirmed(confirmed);
		status.setConfirmedAt(confirmed ? LocalDateTime.now(Period.ZONE) : null);
		periods.save(status);
	}

	/** アルバイト・パートを朝アシシフトの並び順で（未設定の人はユーザー番号順で末尾） */
	private List<Account> staffInOrder() {
		List<Account> staff = new ArrayList<>(accounts.findByRoleIn(Role.STAFF));
		staff.sort(Comparator.comparing(Account::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(Account::getUserNumber));
		return staff;
	}

	private Map<Long, Map<LocalDate, ShiftEntry>> entriesByAccount(Period period) {
		Map<Long, Map<LocalDate, ShiftEntry>> map = new HashMap<>();
		for (ShiftEntry e : entries.findByWorkDateBetween(period.start(), period.end())) {
			map.computeIfAbsent(e.getAccount().getId(), k -> new HashMap<>()).put(e.getWorkDate(), e);
		}
		return map;
	}
}
