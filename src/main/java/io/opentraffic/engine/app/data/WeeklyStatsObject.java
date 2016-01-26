package io.opentraffic.engine.app.data;

import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.data.stats.SummaryStatisticsComparison;

public class WeeklyStatsObject {

	public static double MS_TO_KMH = 3.6d;
	public static int HOURS_IN_WEEK = 24 * 7;
    public static boolean inferred;

	public HourStats[] hours = new HourStats[HOURS_IN_WEEK];

	public WeeklyStatsObject(SummaryStatistics stats) {

        inferred = stats.inferred;

		for(int hour = 0; hour < (HOURS_IN_WEEK); hour++) {

			HourStats hourStats = new HourStats();
			hourStats.h = hour;
			double sum =stats.hourSum.get(hour);
			double count = stats.hourCount.get(hour);
			double std = stats.getStdDev(hour);
			if(!Double.isNaN(sum) && !Double.isNaN(count) && !Double.isNaN(std)){
				hourStats.s = sum;
				hourStats.c = count;
				hourStats.std = std;
			} else {
				hourStats.s = 0;
				hourStats.c = 0;
				hourStats.std = 0;
			}

			hours[hour] = hourStats;
		}
	}

	public WeeklyStatsObject(SummaryStatisticsComparison stats) {

		for(int hour = 0; hour < (HOURS_IN_WEEK); hour++) {

			HourStats hourStats = new HourStats();
			hourStats.h = hour;
			double diff = stats.differenceAsPercent(hour);
			double meanSize = stats.getMeanSize(hour);
			double std = stats.combinedStdDev(hour);
			if(stats.tTest(hour) && !Double.isNaN(diff) && !Double.isNaN(meanSize) && !Double.isNaN(meanSize)) {
				hourStats.s = diff * meanSize;
				hourStats.c = meanSize;
				hourStats.std = std;
			}
			else {
				hourStats.s = 0;
				hourStats.c = stats.getMeanSize(hour);
				hourStats.std = stats.combinedStdDev(hour);
			}




			hours[hour] = hourStats;
		}
	}

	private static class HourStats {
		public int h;
		public double s;
		public double c;
		public double std;
		public boolean t;
	}
}