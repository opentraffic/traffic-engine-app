package io.opentraffic.engine.app.data;

import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.geom.StreetSegment;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kpw on 7/19/15.
 */
public class TrafficPath {

    public Double averageSpeedForRouteInKph;

    public List<TrafficPathEdge> pathEdges = new ArrayList<>();

    public WeeklyStatsObject weeklyStats;

    public void setWeeklyStats(SummaryStatistics stats) {
        weeklyStats = new WeeklyStatsObject(stats);
    }

    public void setWeeklyStats(WeeklyStatsObject stats) {
        weeklyStats = stats;
    }

    public void addSegment(StreetSegment streetSegment, SummaryStatistics summaryStatistics, Color color) {

        TrafficPathEdge pathEdge = new TrafficPathEdge(streetSegment, summaryStatistics, color);
        pathEdges.add(pathEdge);
    }
}
