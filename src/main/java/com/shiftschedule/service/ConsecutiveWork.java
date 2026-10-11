package com.shiftschedule.service;

import java.util.ArrayList;
import java.util.List;

/** 連勤（休みを挟まない出勤の連続）の計算。半月や期の区切りに関係なく、日付の並びで通して数える */
public final class ConsecutiveWork {

	/** この日数を超える連勤（7連勤以上）は警告の対象 */
	public static final int MAX_DAYS = 6;

	private ConsecutiveWork() {
	}

	/**
	 * 各日が属する連勤の長さを返す。休みの日は 0。
	 *
	 * @param carryIn 先頭の日の直前から続いている出勤日数
	 * @param working 日ごとの出勤か（休みなら false）
	 */
	public static List<Integer> runLengths(int carryIn, List<Boolean> working) {
		int[] lengths = new int[working.size()];
		int i = 0;
		while (i < working.size()) {
			if (!working.get(i)) {
				i++;
				continue;
			}
			int end = i;
			while (end < working.size() && working.get(end)) {
				end++;
			}
			int length = end - i + (i == 0 ? carryIn : 0);
			for (int j = i; j < end; j++) {
				lengths[j] = length;
			}
			i = end;
		}
		List<Integer> result = new ArrayList<>();
		for (int length : lengths) {
			result.add(length);
		}
		return result;
	}
}
