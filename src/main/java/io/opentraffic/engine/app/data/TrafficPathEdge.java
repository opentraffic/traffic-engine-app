package io.opentraffic.engine.app.data;

import io.opentraffic.engine.app.TrafficEngineApp;
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

    private Color[] colors;

    public String geometry;
    public String color;
    public double length;
    public double speed;
    public double stdDev;
    public boolean inferred;

    private static int numberOfBins = Integer.parseInt(TrafficEngineApp.appProps.getProperty("application.numberOfBins"));
    private static double maxSpeedInKph = Double.parseDouble(TrafficEngineApp.appProps.getProperty("application.maxSpeedInKph"));

    public TrafficPathEdge(StreetSegment streetSegment, SummaryStatistics summaryStatistics) {
        this(streetSegment, summaryStatistics, null);
    }

    public TrafficPathEdge(StreetSegment streetSegment, SummaryStatistics summaryStatistics, Color comparisonColor) {

        EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(streetSegment.getGeometry());
        geometry = encodedPolyline.getPoints();

        if(comparisonColor == null){
            colors = ColorBrewer.RdYlBu.getColorPalette(numberOfBins + 1);
            if(summaryStatistics.count < 1){
                color = "#808080";
            }else{
                int colorNum = (int) (numberOfBins / (maxSpeedInKph / (summaryStatistics.getMean() * WeeklyStatsObject.MS_TO_KMH)));
                if(colorNum > numberOfBins)
                    colorNum = numberOfBins;
                color = String.format("#%02x%02x%02x", colors[colorNum].getRed(), colors[colorNum].getGreen(), colors[colorNum].getBlue());
            }
        }else{
            color = String.format("#%02x%02x%02x", comparisonColor.getRed(), comparisonColor.getGreen(), comparisonColor.getBlue());
        }


        length = streetSegment.length;
        speed = summaryStatistics.getMean();
        stdDev = summaryStatistics.getStdDev();
        inferred = summaryStatistics.inferred;
    }
}
