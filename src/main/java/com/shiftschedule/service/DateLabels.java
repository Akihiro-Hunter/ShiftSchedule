package com.shiftschedule.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

/** 日付の表示用ラベル。画面からは ${@dateLabels.xxx(date)} で使う */
@Component("dateLabels")
public class DateLabels {

	private static final String[] WEEK = { "月", "火", "水", "木", "金", "土", "日" };

	/** 曜日（例：木） */
	public String week(LocalDate date) {
		return WEEK[date.getDayOfWeek().getValue() - 1];
	}

	/** 例：10/16(木) */
	public String monthDayWithWeek(LocalDate date) {
		return date.getMonthValue() + "/" + date.getDayOfMonth() + "(" + week(date) + ")";
	}

	/** 土日の色分け用 CSS クラス */
	public String weekClass(LocalDate date) {
		DayOfWeek dow = date.getDayOfWeek();
		if (dow == DayOfWeek.SATURDAY) {
			return "sat";
		}
		if (dow == DayOfWeek.SUNDAY) {
			return "sun";
		}
		return "";
	}
}
