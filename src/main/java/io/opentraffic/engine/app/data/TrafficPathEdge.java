package io.opentraffic.engine.app.data;

import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.geom.StreetSegment;
import org.jcolorbrewer.ColorBrewer;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

import java.awt.*;

/**
 * Created by kpw on 7/19/15.
 */
public class TrafficPathEdge {

    static Color[] colors = ColorBrewer.RdYlBu.getColorPalette(11);

    public String geometry;
    public String color;
    public double length;
    public double speed;
    public double stdDev;

    public TrafficPathEdge(StreetSegment streetSegment, SummaryStatistics summaryStatistics) {


        EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(streetSegment.getGeometry());
        geometry = encodedPolyline.getPoints();

        int colorNum = (int) (10 / (50.0 / (summaryStatistics.getMean() * 3.6)));
        if(colorNum > 10)
            colorNum = 10;

        color = String.format("#%02x%02x%02x", colors[colorNum].getRed(), colors[colorNum].getGreen(), colors[colorNum].getBlue());

        length = streetSegment.length;
        speed = summaryStatistics.getMean();
        stdDev = summaryStatistics.getStdDev();
    }
}
