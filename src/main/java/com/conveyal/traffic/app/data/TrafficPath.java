package com.conveyal.traffic.app.data;

import com.conveyal.traffic.data.stats.SummaryStatistics;
import com.conveyal.traffic.geom.StreetSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kpw on 7/19/15.
 */
public class TrafficPath {

    public List<TrafficPathEdge> pathEdges = new ArrayList<>();

    public void addSegment(StreetSegment streetSegment, SummaryStatistics summaryStatistics) {

        TrafficPathEdge pathEdge = new TrafficPathEdge(streetSegment, summaryStatistics);
        pathEdges.add(pathEdge);
    }
}
