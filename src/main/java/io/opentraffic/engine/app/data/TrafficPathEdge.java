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

    private static int numberOfBins = Integer.parseInt(TrafficEngineApp.appProps.getProperty("application.numberOfBins"));
    private static double maxSpeedInKph = Double.parseDouble(TrafficEngineApp.appProps.getProperty("application.maxSpeedInKph"));

    public TrafficPathEdge(StreetSegment streetSegment, SummaryStatistics summaryStatistics) {
        colors = ColorBrewer.RdYlBu.getColorPalette(numberOfBins + 1);

        EncodedPolylineBean encodedPolyline = PolylineEncoder.createEncodings(streetSegment.getGeometry());
        geometry = encodedPolyline.getPoints();

        if(summaryStatistics.count < 1){
            color = "#808080";
        }else{
            int colorNum = (int) (numberOfBins / (maxSpeedInKph / (summaryStatistics.getMean() * 3.6)));
            if(colorNum > numberOfBins)
                colorNum = numberOfBins;
            color = String.format("#%02x%02x%02x", colors[colorNum].getRed(), colors[colorNum].getGreen(), colors[colorNum].getBlue());
        }

        length = streetSegment.length;
        speed = summaryStatistics.getMean();
        stdDev = summaryStatistics.getStdDev();
    }
}
