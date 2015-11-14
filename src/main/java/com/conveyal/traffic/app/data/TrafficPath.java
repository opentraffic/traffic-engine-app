package com.conveyal.traffic.app.data;

import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.geom.StreetSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kpw on 7/19/15.
 */
public class TrafficPath {

    public List<TrafficPathEdge> pathEdges = new ArrayList<>();

    public WeeklyStatsObject weeklyStats;

    public void setWeeklyStats(SummaryStatistics stats) {
        weeklyStats = new WeeklyStatsObject(stats);
    }

    public void addSegment(StreetSegment streetSegment, SummaryStatistics summaryStatistics) {

        TrafficPathEdge pathEdge = new TrafficPathEdge(streetSegment, summaryStatistics);
        pathEdges.add(pathEdge);
    }
}
