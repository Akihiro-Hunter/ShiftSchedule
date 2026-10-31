package com.shiftschedule.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shiftschedule.domain.Account;
import com.shiftschedule.domain.DailyPlan;
import com.shiftschedule.domain.Half;
import com.shiftschedule.domain.Period;
import com.shiftschedule.domain.RequestStatus;
import com.shiftschedule.domain.RequirementKind;
import com.shiftschedule.domain.ShiftAssignment;
import com.shiftschedule.domain.ShiftEntry;
import com.shiftschedule.domain.ShiftLock;
import com.shiftschedule.domain.ShiftRequest;
import com.shiftschedule.domain.WorkTask;
import com.shiftschedule.repository.AccountRepository;
import com.shiftschedule.repository.DailyPlanRepository;
import com.shiftschedule.repository.ShiftAssignmentRepository;
import com.shiftschedule.repository.ShiftEntryRepository;
import com.shiftschedule.repository.ShiftLockRepository;
import com.shiftschedule.repository.ShiftRequestRepository;
import com.shiftschedule.repository.WorkTaskRepository;

/**
 * 勤務表の自動作成。鍵のかかっていない人の、確定前の半月の出勤・休みと作業の割り当てを作り直す。
 * 実際の計算は {@link ShiftGenerator} が行う。
 */
@Service
@Transactional
public class ShiftGenerateService {

	/** 期間の前の日のうち、連勤・週の数え直しのために見る日数 */
	private static final int LOOKBACK_DAYS = 7;

	private final AccountRepository accounts;
	private final ShiftRequestRepository requests;
	private final ShiftEntryRepository entries;
	private final ShiftLockRepository locks;
	private final DailyPlanRepository plans;
	private final WorkTaskRepository tasks;
	private final ShiftAssignmentRepository assignments;
	private final ShiftService shiftService;

	public ShiftGenerateService(AccountRepository accounts, ShiftRequestRepository requests,
			ShiftEntryRepository entries, ShiftLockRepository locks, DailyPlanRepository plans,
			WorkTaskRepository tasks, ShiftAssignmentRepository assignments, ShiftService shiftService) {
		this.accounts = accounts;
		this.requests = requests;
		this.entries = entries;
		this.locks = locks;
		this.plans = plans;
		this.tasks = tasks;
		this.assignments = assignments;
		this.shiftService = shiftService;
	}

	/**
	 * @param regeneratedDays 作り直した人・日の数
	 * @param capacity        契約日数の範囲で出勤できる延べ人日の目安（職員の契約日数÷7×期間の日数）
	 * @param planned         期間の予定人数の合計
	 * @param optimal         時間内に最良の案を見つけ切ったか（false は、見つかった中で一番良い案）
	 */
	public record Outcome(int regeneratedDays, int capacity, int planned, boolean optimal) {

		/** 契約日数の範囲では予定人数に届かない（人手が足りない日が必ず出る） */
		public boolean isUnderstaffed() {
			return capacity < planned;
		}
	}

	/**
	 * 期間の勤務表を作り直す。
	 * 鍵のかかっている人、確定済みの半月、休み希望・年休の日は変えない。
	 */
	public Outcome generate(Period period) {
		List<LocalDate> dates = period.dates();
		LocalDate lookbackFrom = period.start().minusDays(LOOKBACK_DAYS);

		List<Account> staff = shiftService.staffInOrder();
		Set<Long> lockedIds = new HashSet<>();
		for (ShiftLock lock : locks.findByPeriodStart(period.start())) {
			lockedIds.add(lock.getAccount().getId());
		}
		Set<LocalDate> confirmedDates = new HashSet<>();
		for (Half half : Half.values()) {
			if (shiftService.isConfirmed(period, half)) {
				confirmedDates.addAll(period.dates(half));
			}
		}

		Map<Long, Set<LocalDate>> requestedOff = new HashMap<>();
		for (ShiftRequest r : requests.findByWorkDateBetween(lookbackFrom, period.end())) {
			if (r.getStatus() == RequestStatus.OFF) {
				requestedOff.computeIfAbsent(r.getAccount().getId(), k -> new HashSet<>()).add(r.getWorkDate());
			}
		}
		Map<Long, Map<LocalDate, ShiftEntry>> entryMap = new HashMap<>();
		for (ShiftEntry e : entries.findByWorkDateBetween(lookbackFrom, period.end())) {
			entryMap.computeIfAbsent(e.getAccount().getId(), k -> new HashMap<>()).put(e.getWorkDate(), e);
		}
		List<ShiftAssignment> existing = assignments.findByWorkDateBetween(period.start(), period.end());

		Map<Long, Set<LocalDate>> freeDays = new HashMap<>();
		Map<Long, Map<LocalDate, Boolean>> fixedWorking = new HashMap<>();
		for (Account account : staff) {
			Set<LocalDate> off = requestedOff.getOrDefault(account.getId(), Set.of());
			Map<LocalDate, ShiftEntry> own = entryMap.getOrDefault(account.getId(), Map.of());
			Set<LocalDate> free = new HashSet<>();
			Map<LocalDate, Boolean> fixed = new HashMap<>();
			for (LocalDate date = lookbackFrom; !date.isAfter(period.end()); date = date.plusDays(1)) {
				ShiftEntry entry = own.get(date);
				boolean entryOff = entry != null && entry.isDayOff();
				if (off.contains(date)) {
					fixed.put(date, false);
				} else if (date.isBefore(period.start())) {
					fixed.put(date, !entryOff);
				} else if (lockedIds.contains(account.getId()) || confirmedDates.contains(date)) {
					fixed.put(date, !entryOff);
				} else if (entryOff && entry.isPaidLeave()) {
					fixed.put(date, false);
				} else {
					free.add(date);
				}
			}
			freeDays.put(account.getId(), free);
			fixedWorking.put(account.getId(), fixed);
		}

		List<ShiftGenerator.Assignment> fixedAssigned = new ArrayList<>();
		for (ShiftAssignment a : existing) {
			Long id = a.getAccount().getId();
			if (!freeDays.getOrDefault(id, Set.of()).contains(a.getWorkDate())
					&& fixedWorking.getOrDefault(id, Map.of()).getOrDefault(a.getWorkDate(), true)) {
				fixedAssigned.add(new ShiftGenerator.Assignment(id, a.getWorkDate(), a.getTask().getId()));
			}
		}

		Map<LocalDate, Integer> planned = new HashMap<>();
		for (DailyPlan plan : plans.findByWorkDateBetween(period.start(), period.end())) {
			planned.put(plan.getWorkDate(), plan.getPlannedCount());
		}
		for (LocalDate date : dates) {
			planned.putIfAbsent(date, DailyPlan.defaultCount(date));
		}

		List<ShiftGenerator.TaskNeed> needs = tasks.findAll().stream().filter(t -> t.getRequiredCount() > 0)
				.map(t -> new ShiftGenerator.TaskNeed(t.getId(), t.getGroup().getId(),
						t.getRequiredKind() == RequirementKind.REQUIRED, t.getRequiredCount()))
				.toList();
		List<ShiftGenerator.StaffInfo> staffInfos = staff.stream()
				.map(a -> new ShiftGenerator.StaffInfo(a.getId(), a.getContractDays() == null ? 5 : a.getContractDays(),
						a.getTasks().stream().map(WorkTask::getId).collect(Collectors.toSet())))
				.toList();

		ShiftGenerator.Result result = ShiftGenerator.solve(new ShiftGenerator.Problem(dates, staffInfos, needs,
				freeDays, fixedWorking, fixedAssigned, planned));

		return apply(period, staff, freeDays, result, existing, entryMap, planned);
	}

	private Outcome apply(Period period, List<Account> staff, Map<Long, Set<LocalDate>> freeDays,
			ShiftGenerator.Result result, List<ShiftAssignment> existing,
			Map<Long, Map<LocalDate, ShiftEntry>> entryMap, Map<LocalDate, Integer> plannedByDate) {
		// 作り直す日の古い割り当てを消す（同じ組み合わせを入れ直すため、先に消して反映する）
		List<ShiftAssignment> stale = existing.stream()
				.filter(a -> freeDays.getOrDefault(a.getAccount().getId(), Set.of()).contains(a.getWorkDate()))
				.toList();
		assignments.deleteAll(stale);
		assignments.flush();

		Map<Long, WorkTask> taskMap = tasks.findAll().stream().collect(Collectors.toMap(WorkTask::getId, t -> t));
		int days = 0;
		for (Account account : staff) {
			Map<LocalDate, Boolean> working = result.working().getOrDefault(account.getId(), Map.of());
			Map<LocalDate, ShiftEntry> own = entryMap.getOrDefault(account.getId(), Map.of());
			for (Map.Entry<LocalDate, Boolean> e : working.entrySet()) {
				LocalDate date = e.getKey();
				if (!period.contains(date)) {
					continue;
				}
				days++;
				ShiftEntry entry = own.get(date);
				if (e.getValue()) {
					if (entry != null) {
						if (entry.getNote() == null || entry.getNote().isEmpty()) {
							entries.delete(entry);
						} else {
							entry.setDayOff(false);
							entry.setPaidLeave(false);
						}
					}
				} else {
					if (entry == null) {
						entry = new ShiftEntry(account, date);
					}
					entry.setDayOff(true);
					entry.setPaidLeave(false);
					entries.save(entry);
				}
			}
		}
		for (ShiftGenerator.Assignment a : result.assignments()) {
			assignments.save(new ShiftAssignment(accounts.getReferenceById(a.staffId()), a.date(), taskMap.get(a.taskId())));
		}
		int capacity = (int) Math.floor(staff.stream()
				.mapToDouble(a -> (a.getContractDays() == null ? 5 : a.getContractDays()) * period.dates().size() / 7.0).sum());
		int planned = period.dates().stream().mapToInt(d -> plannedByDate.getOrDefault(d, 0)).sum();
		return new Outcome(days, capacity, planned, "OPTIMAL".equals(result.status()));
	}

	/** 作業の割り当てと、作業ごとの必要人数に対する充足の表 */
	public record AssignmentView(List<LocalDate> dates, List<CoverageRow> coverage, List<StaffRow> staff,
			List<String> shortages) {

		public boolean isEmpty() {
			return coverage.isEmpty();
		}
	}

	/**
	 * @param counts 日ごとの割り当て人数（dates と同じ順）
	 */
	public record CoverageRow(String groupName, String taskName, boolean required, int need, List<Integer> counts) {

		/** 画面表示用（必須 / 推奨） */
		public String kindLabel() {
			return required ? RequirementKind.REQUIRED.getLabel() : RequirementKind.PREFERRED.getLabel();
		}

		public boolean isShort(int index) {
			return counts.get(index) < need;
		}
	}

	/**
	 * @param cells 日ごとの作業名（休み・割り当てなしは空文字。dates と同じ順）
	 */
	public record StaffRow(String name, List<String> cells) {
	}

	/** 期間の割り当てと不足を組み立てる。休みの日の割り当ては数えない */
	@Transactional(readOnly = true)
	public AssignmentView buildView(Period period, ShiftSheet sheet) {
		List<LocalDate> dates = period.dates();
		Set<String> offKeys = new HashSet<>();
		for (ShiftSheet.Row row : sheet.rows()) {
			for (ShiftSheet.Cell cell : row.cells()) {
				if (cell.dayOff()) {
					offKeys.add(row.accountId() + "/" + cell.date());
				}
			}
		}
		Map<String, List<WorkTask>> byStaffDay = new HashMap<>();
		Map<String, Integer> counts = new HashMap<>();
		for (ShiftAssignment a : assignments.findByWorkDateBetween(period.start(), period.end())) {
			String key = a.getAccount().getId() + "/" + a.getWorkDate();
			if (offKeys.contains(key)) {
				continue;
			}
			byStaffDay.computeIfAbsent(key, k -> new ArrayList<>()).add(a.getTask());
			counts.merge(a.getTask().getId() + "/" + a.getWorkDate(), 1, Integer::sum);
		}

		Comparator<WorkTask> order = Comparator.comparingInt((WorkTask t) -> t.getGroup().getSortOrder())
				.thenComparing(t -> t.getGroup().getId()).thenComparingInt(WorkTask::getSortOrder)
				.thenComparing(WorkTask::getId);
		List<CoverageRow> coverage = new ArrayList<>();
		List<String> shortages = new ArrayList<>();
		for (WorkTask task : tasks.findAll().stream().filter(t -> t.getRequiredCount() > 0).sorted(order).toList()) {
			boolean required = task.getRequiredKind() == RequirementKind.REQUIRED;
			List<Integer> rowCounts = new ArrayList<>();
			for (LocalDate date : dates) {
				rowCounts.add(counts.getOrDefault(task.getId() + "/" + date, 0));
			}
			CoverageRow row = new CoverageRow(task.getGroup().getName(), task.getName(), required,
					task.getRequiredCount(), rowCounts);
			coverage.add(row);
			if (required) {
				for (int i = 0; i < dates.size(); i++) {
					if (row.isShort(i)) {
						shortages.add(dates.get(i).getMonthValue() + "/" + dates.get(i).getDayOfMonth() + " "
								+ task.getName() + "（" + (task.getRequiredCount() - rowCounts.get(i)) + "人不足）");
					}
				}
			}
		}

		List<StaffRow> staffRows = new ArrayList<>();
		for (ShiftSheet.Row row : sheet.rows()) {
			List<String> cells = new ArrayList<>();
			for (LocalDate date : dates) {
				List<WorkTask> list = byStaffDay.getOrDefault(row.accountId() + "/" + date, List.of());
				cells.add(list.stream().sorted(order).map(WorkTask::getName).collect(Collectors.joining("・")));
			}
			staffRows.add(new StaffRow(row.name(), cells));
		}
		return new AssignmentView(dates, coverage, staffRows, shortages);
	}
}
