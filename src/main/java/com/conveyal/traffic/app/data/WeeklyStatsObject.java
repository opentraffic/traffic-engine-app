package com.conveyal.traffic.app.data;

import com.conveyal.traffic.data.stats.SummaryStatistics;

public class WeeklyStatsObject {

	public static double MS_TO_KMH = 3.6d;
	public static int HOURS_IN_WEEK = 24 * 7;

	public HourStats[] hours = new HourStats[HOURS_IN_WEEK];

	public WeeklyStatsObject(SummaryStatistics stats) {

		for(int hour = 0; hour < (HOURS_IN_WEEK); hour++) {

			HourStats hourStats = new HourStats();
			hourStats.h = hour;
			hourStats.s = stats.hourSum.get(hour);
			hourStats.c = stats.hourCount.get(hour);
			hourStats.std = stats.getStdDev(hour);

			hours[hour] = hourStats;
		}
	}

	private static class HourStats {
		public int h;
		public double s;
		public double c;
		public double std;
	}
}