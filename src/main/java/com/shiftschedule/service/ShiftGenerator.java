package com.shiftschedule.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.ortools.Loader;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.CpSolver;
import com.google.ortools.sat.CpSolverStatus;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.LinearArgument;
import com.google.ortools.sat.LinearExpr;
import com.google.ortools.sat.LinearExprBuilder;

/**
 * 勤務表の自動作成（制約付き最適化）。DB には触れず、与えられた条件だけで割り当てを決める。
 *
 * <p>必ず守る条件：可能作業のない作業には入れない／同じ時間帯（大項目）には1日1作業まで／
 * 週（月〜日）の出勤日数は契約日数以下。<br>
 * できるだけ満たす条件（減点）：作業ごとの必要人数（必須は特に重い）／7連勤以上を避ける／
 * 契約日数を下回らない／出勤人数を予定人数に合わせる。
 */
public final class ShiftGenerator {

	/** 必須の必要人数が1人足りないごとの減点 */
	static final int PENALTY_REQUIRED_SHORT = 1000;
	/** 「推奨」の必要人数が1人足りないごとの減点 */
	static final int PENALTY_PREFERRED_SHORT = 100;
	/** 作業の割り当て1つごとの減点（必要人数を超えて割り当てないための、ごく小さな値） */
	static final int PENALTY_ASSIGNMENT = 1;
	/** 7連勤以上になる窓1つごとの減点 */
	static final int PENALTY_LONG_RUN = 30;
	/** 予定人数との差の基本の減点（足りない場合は人数が増えるほど重くなる） */
	static final int PENALTY_PLAN_GAP = 15;
	/** 予定人数に足りない人数を、何人目まで段階的に重く減点するか（それ以降は同じ重さ） */
	static final int PLAN_SHORT_STEPS = 5;
	/** 完全な週で契約日数に1日足りないごとの減点 */
	static final int PENALTY_BELOW_CONTRACT = 5;

	/** 連勤として警告する日数（この日数以上続けて出勤すると減点） */
	static final int RUN_LIMIT = 7;
	/** 解く時間の上限（秒） */
	static final double TIME_LIMIT_SECONDS = 20.0;

	private ShiftGenerator() {
	}

	/** 必要人数のある作業 */
	public record TaskNeed(long taskId, long groupId, boolean required, int need) {
	}

	public record StaffInfo(long id, int contractDays, Set<Long> taskIds) {
	}

	public record Assignment(long staffId, LocalDate date, long taskId) {
	}

	/**
	 * @param dates          作成する期間の日付（連続した日）
	 * @param freeDays       作り直してよい日（職員ごと）
	 * @param fixedWorking   作り直さない日の出勤（true）・休み（false）。期間の前（連勤・週の数え直し用）も含む。
	 *                       どちらにも無い日は、期間内なら出勤、期間の前後なら休みとして扱う
	 * @param fixedAssigned  作り直さない日の、すでにある作業の割り当て
	 * @param planned        日ごとの予定人数
	 */
	public record Problem(List<LocalDate> dates, List<StaffInfo> staff, List<TaskNeed> needs,
			Map<Long, Set<LocalDate>> freeDays, Map<Long, Map<LocalDate, Boolean>> fixedWorking,
			List<Assignment> fixedAssigned, Map<LocalDate, Integer> planned) {
	}

	/**
	 * @param working     作り直した日の出勤（true）・休み（false）
	 * @param assignments 作り直した日の作業の割り当て
	 */
	public record Result(Map<Long, Map<LocalDate, Boolean>> working, List<Assignment> assignments, String status) {
	}

	private record Key(long staffId, LocalDate date) {
	}

	public static Result solve(Problem problem) {
		Loader.loadNativeLibraries();
		List<LocalDate> dates = problem.dates();
		LocalDate first = dates.get(0);
		LocalDate last = dates.get(dates.size() - 1);

		CpModel model = new CpModel();
		Map<Key, BoolVar> work = new HashMap<>();
		Map<Key, Map<Long, BoolVar>> assign = new HashMap<>();
		List<LinearArgument> penaltyVars = new ArrayList<>();
		List<Long> penaltyWeights = new ArrayList<>();

		// 出勤の変数と、作業の割り当ての変数
		for (StaffInfo staff : problem.staff()) {
			for (LocalDate date : problem.freeDays().getOrDefault(staff.id(), Set.of())) {
				Key key = new Key(staff.id(), date);
				BoolVar x = model.newBoolVar("w_" + staff.id() + "_" + date);
				work.put(key, x);
				Map<Long, List<BoolVar>> byGroup = new HashMap<>();
				Map<Long, BoolVar> perTask = new HashMap<>();
				for (TaskNeed need : problem.needs()) {
					if (!staff.taskIds().contains(need.taskId())) {
						continue;
					}
					BoolVar a = model.newBoolVar("a_" + staff.id() + "_" + date + "_" + need.taskId());
					model.addImplication(a, x);
					perTask.put(need.taskId(), a);
					// 必要人数を超える割り当ては作らない（ごく小さな減点）
					penaltyVars.add(a);
					penaltyWeights.add((long) PENALTY_ASSIGNMENT);
					byGroup.computeIfAbsent(need.groupId(), g -> new ArrayList<>()).add(a);
				}
				// 同じ時間帯（大項目）には1日1作業まで
				for (List<BoolVar> vars : byGroup.values()) {
					if (vars.size() > 1) {
						model.addAtMostOne(vars.toArray(new BoolVar[0]));
					}
				}
				assign.put(key, perTask);
			}
		}

		// 週（月〜日）ごとの契約日数。上限は必ず守り、完全な週で下回るのは減点
		for (StaffInfo staff : problem.staff()) {
			LocalDate weekStart = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			for (; !weekStart.isAfter(last); weekStart = weekStart.plusDays(7)) {
				LinearExprBuilder sum = LinearExpr.newBuilder();
				int fixedCount = 0;
				boolean hasVar = false;
				boolean complete = true;
				for (int i = 0; i < 7; i++) {
					LocalDate date = weekStart.plusDays(i);
					BoolVar x = work.get(new Key(staff.id(), date));
					if (x != null) {
						sum.add(x);
						hasVar = true;
					} else if (!date.isAfter(last)) {
						if (isWorking(problem, staff.id(), date)) {
							fixedCount++;
						}
					} else {
						complete = false;
					}
				}
				if (!hasVar) {
					continue;
				}
				// すでに固定の日だけで契約日数を超えている週は、増やさないことだけ守る
				long cap = Math.max(staff.contractDays(), fixedCount);
				model.addLessOrEqual(sum, cap - fixedCount);
				if (complete) {
					IntVar below = model.newIntVar(0, 7, "below_" + staff.id() + "_" + weekStart);
					// 出勤 + 不足 >= 契約日数
					LinearExprBuilder total = LinearExpr.newBuilder();
					total.add(sum.build());
					total.add(below);
					model.addGreaterOrEqual(total, staff.contractDays() - fixedCount);
					penaltyVars.add(below);
					penaltyWeights.add((long) PENALTY_BELOW_CONTRACT);
				}
			}
		}

		// 7連勤以上を避ける（期間の前の日も含めて数える）
		for (StaffInfo staff : problem.staff()) {
			for (LocalDate start = first.minusDays(RUN_LIMIT - 1); !start.plusDays(RUN_LIMIT - 1).isAfter(last); start = start.plusDays(1)) {
				LinearExprBuilder sum = LinearExpr.newBuilder();
				boolean hasVar = false;
				for (int i = 0; i < RUN_LIMIT; i++) {
					LocalDate date = start.plusDays(i);
					BoolVar x = work.get(new Key(staff.id(), date));
					if (x != null) {
						sum.add(x);
						hasVar = true;
					} else if (isWorking(problem, staff.id(), date)) {
						sum.add(1);
					}
				}
				if (!hasVar) {
					continue;
				}
				BoolVar run = model.newBoolVar("run_" + staff.id() + "_" + start);
				sum.addTerm(run, -1);
				model.addLessOrEqual(sum, RUN_LIMIT - 1);
				penaltyVars.add(run);
				penaltyWeights.add((long) PENALTY_LONG_RUN);
			}
		}

		// 作業ごとの必要人数
		for (LocalDate date : dates) {
			for (TaskNeed need : problem.needs()) {
				LinearExprBuilder supply = LinearExpr.newBuilder();
				for (StaffInfo staff : problem.staff()) {
					BoolVar a = assign.getOrDefault(new Key(staff.id(), date), Map.of()).get(need.taskId());
					if (a != null) {
						supply.add(a);
					}
				}
				long fixed = problem.fixedAssigned().stream()
						.filter(f -> f.date().equals(date) && f.taskId() == need.taskId()).count();
				IntVar shortage = model.newIntVar(0, need.need(), "short_" + date + "_" + need.taskId());
				supply.add(shortage);
				model.addGreaterOrEqual(supply, need.need() - fixed);
				penaltyVars.add(shortage);
				penaltyWeights.add((long) (need.required() ? PENALTY_REQUIRED_SHORT : PENALTY_PREFERRED_SHORT));
			}
		}

		// 出勤人数を予定人数に合わせる
		int staffCount = problem.staff().size();
		for (LocalDate date : dates) {
			Integer plan = problem.planned().get(date);
			if (plan == null) {
				continue;
			}
			LinearExprBuilder total = LinearExpr.newBuilder();
			for (StaffInfo staff : problem.staff()) {
				BoolVar x = work.get(new Key(staff.id(), date));
				if (x != null) {
					total.add(x);
				} else if (isWorking(problem, staff.id(), date)) {
					total.add(1);
				}
			}
			// 足りない人数は、1人目より2人目、2人目より3人目…と重く減点する。
			// 人手が足りない日は、数日に集中させず、できるだけ均等に分けるため
			for (int k = 1; k <= PLAN_SHORT_STEPS; k++) {
				IntVar unit = model.newIntVar(0, 1, "under_" + date + "_" + k);
				total.add(unit);
				penaltyVars.add(unit);
				penaltyWeights.add((long) PENALTY_PLAN_GAP * k);
			}
			IntVar rest = model.newIntVar(0, staffCount, "under_rest_" + date);
			total.add(rest);
			penaltyVars.add(rest);
			penaltyWeights.add((long) PENALTY_PLAN_GAP * PLAN_SHORT_STEPS);
			IntVar over = model.newIntVar(0, staffCount, "over_" + date);
			total.addTerm(over, -1);
			penaltyVars.add(over);
			penaltyWeights.add((long) PENALTY_PLAN_GAP);
			model.addEquality(total, plan);
		}

		model.minimize(LinearExpr.weightedSum(penaltyVars.toArray(new LinearArgument[0]),
				penaltyWeights.stream().mapToLong(Long::longValue).toArray()));

		CpSolver solver = new CpSolver();
		solver.getParameters().setMaxTimeInSeconds(TIME_LIMIT_SECONDS);
		solver.getParameters().setNumWorkers(8);
		CpSolverStatus status = solver.solve(model);
		if (status != CpSolverStatus.OPTIMAL && status != CpSolverStatus.FEASIBLE) {
			throw new IllegalStateException("勤務表を作成できませんでした（" + status + "）");
		}

		Map<Long, Map<LocalDate, Boolean>> working = new HashMap<>();
		List<Assignment> assignments = new ArrayList<>();
		work.forEach((key, x) -> {
			working.computeIfAbsent(key.staffId(), k -> new HashMap<>()).put(key.date(), solver.booleanValue(x));
			assign.get(key).forEach((taskId, a) -> {
				if (solver.booleanValue(a)) {
					assignments.add(new Assignment(key.staffId(), key.date(), taskId));
				}
			});
		});
		return new Result(working, assignments, status.name());
	}

	/** 作り直さない日の出勤。情報のない日は、期間内なら出勤、期間の前後（不明な日）なら休みとして扱う */
	private static boolean isWorking(Problem problem, long staffId, LocalDate date) {
		List<LocalDate> dates = problem.dates();
		boolean inPeriod = !date.isBefore(dates.get(0)) && !date.isAfter(dates.get(dates.size() - 1));
		return problem.fixedWorking().getOrDefault(staffId, Map.of()).getOrDefault(date, inPeriod);
	}
}
