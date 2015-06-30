package com.conveyal.traffic.app.data;

import com.conveyal.traffic.stats.SegmentStatistics;

import java.util.Arrays;

public class WeeklyStatsObject {

	public static double MS_TO_KMH = 3.6d;

	public Double[]  dailyStats = new Double[7];
	public Double[]  hourlyStats = new Double[7 * 24];

	public WeeklyStatsObject(SegmentStatistics stats) {

		Arrays.fill(dailyStats, 0.0);
		Arrays.fill(hourlyStats, 0.0);

		long dailyCount = 0l;
		double dailySum = 0.0;

		int day = 0;
		for(int hour = 0; hour < (24 * 7); hour++) {

			if(stats.hourSampleCount[hour] > 0) {
				hourlyStats[hour] = (stats.hourSampleSum[hour] / stats.hourSampleCount[hour]) * MS_TO_KMH;

				dailyCount += stats.hourSampleCount[hour];
				dailySum += stats.hourSampleSum[hour];
			}

			if(day != ((hour - (hour % 24)) / 24)) {

				if(dailyCount > 0)
					dailyStats[day] = (dailySum / dailyCount)  * MS_TO_KMH;

				dailySum = 0.0;
				dailyCount = 0l;

				day = ((hour - (hour % 24)) / 24);
			}
		}

		if(dailyCount > 0)
			dailyStats[day] = dailySum / dailyCount  * MS_TO_KMH;

	}
}