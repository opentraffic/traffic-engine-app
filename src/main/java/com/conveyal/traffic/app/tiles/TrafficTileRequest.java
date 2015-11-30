package com.conveyal.traffic.app.tiles;

import io.opentraffic.engine.data.SpatialDataItem;
import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.data.stats.SummaryStatisticsComparison;
import io.opentraffic.engine.geom.StreetSegment;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.imaging.ImageWriteException;
import org.jcolorbrewer.ColorBrewer;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;

import com.conveyal.traffic.app.TrafficEngineApp;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.vividsolutions.jts.geom.GeometryFactory;

public abstract class TrafficTileRequest {

    public static final double MS_TO_KMS = 3.6d;

    final public String type;
    final public Integer x, y, z;

    public TrafficTileRequest(Integer x, Integer y, Integer z, String type) {
	this.x = x;
	this.y = y;
	this.z = z;

	this.type = type;
    }

    public String getId() {
	return type + "_" + x + "_" + y + "_" + z;
    }

    public boolean equals(TrafficTileRequest tr) {
	return this.getId().equals(tr.getId());
    }

    @Override
    public int hashCode() {
	HashFunction hashFunction = Hashing.md5();
	HashCode hashCode = hashFunction.newHasher()
		.putString(this.getId(), Charsets.UTF_8).hash();

	return hashCode.asInt();
    }

    abstract byte[] render();

    public static class SegmentTile extends TrafficTileRequest {
	Set<Integer> hours;
	Set<Integer> w1, w2;

	Boolean normalizeByTime;
	Integer confidenceInterval;

	public SegmentTile(Integer x, Integer y, Integer z,
		Boolean normalizeByTime, Integer confidenceInterval,
		List<Integer> w1, List<Integer> w2, List<Integer> hours) {
	    super(x, y, z, "segment");

	    this.hours = new HashSet<>(hours);
	    this.w1 = new HashSet<>(w1);
	    this.w2 = new HashSet<>(w2);
	    this.normalizeByTime = normalizeByTime;
	    this.confidenceInterval = confidenceInterval;
	}

	@Override
	public String getId() {
	    return super.getId();
	}

	@Override
	public byte[] render() {
	    if (this.w2 != null && this.w2.size() > 0)
		return renderPercentChange();
	    else
		return renderSpeed();
	}

	public byte[] renderPercentChange() {

	    Tile tile = new Tile(this);

	    List<Long> segmentIds = TrafficEngineApp.engine.getTrafficEngine()
		    .getStreetSegmentIds(tile.envelope);

	    for (Long id : segmentIds) {

		try {

		    double averageSpeed = 0.0;

		    int streetType = TrafficEngineApp.engine.getTrafficEngine()
			    .getStreetTypeById(id);

		    if (streetType == StreetSegment.TYPE_PRIMARY && z < 11)
			continue;
		    else if (streetType == StreetSegment.TYPE_SECONDARY
			    && z < 14)
			continue;
		    else if ((streetType == StreetSegment.TYPE_TERTIARY)
			    && z < 15)
			continue;
		    else if ((streetType == StreetSegment.TYPE_RESIDENTIAL)
			    && z < 17)
			continue;
		    else if (streetType == StreetSegment.TYPE_OTHER
			    || streetType == StreetSegment.TYPE_NON_ROADWAY)
			continue;

		    Color color;

		    SummaryStatistics stats1 = TrafficEngineApp.engine
			    .getTrafficEngine().osmData.statsDataStore
			    .collectSummaryStatistics(id, normalizeByTime, w1,
				    hours);
		    SummaryStatistics stats2 = TrafficEngineApp.engine
			    .getTrafficEngine().osmData.statsDataStore
			    .collectSummaryStatistics(id, normalizeByTime, w2,
				    hours);

		    SummaryStatisticsComparison statsComparison = new SummaryStatisticsComparison(
			    SummaryStatisticsComparison.PValue.values()[confidenceInterval],
			    stats1, stats2);

		    Color[] colors;

		    if (statsComparison.tTest()) {

			double speedPercentChange = statsComparison
				.differenceAsPercent();

			if (speedPercentChange < 0)
			    colors = ColorBrewer.Reds.getColorPalette(5);
			else
			    colors = ColorBrewer.Blues.getColorPalette(5);

			int colorNum;

			if (Math.abs(speedPercentChange) > .5)
			    colorNum = 4;
			else {
			    speedPercentChange = Math.abs(speedPercentChange) / 0.5;
			    colorNum = (int) Math.round(4 * speedPercentChange);
			}

			tile.renderLineString(TrafficEngineApp.engine
				.getTrafficEngine().getGeometryById(id),
				colors[colorNum], 2);

		    }
		    // else
		    // tile.renderLineString(TrafficEngineApp.engine.getTrafficEngine().getGeometryById(id),
		    // Color.GREEN, 2);

		} catch (MismatchedDimensionException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		} catch (TransformException e) {
		    e.printStackTrace();
		}
	    }

	    try {
		return tile.generateImage();
	    } catch (IOException | ImageWriteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	    }
	}

	public byte[] renderSpeed() {

	    Tile tile = new Tile(this);

	    List<Long> segmentIds = TrafficEngineApp.engine.getTrafficEngine()
		    .getStreetSegmentIds(tile.envelope);

	    Color[] colors = ColorBrewer.RdYlBu.getColorPalette(11);

	    for (Long id : segmentIds) {

		try {

		    double averageSpeed = 0.0;

		    int streetType = TrafficEngineApp.engine.getTrafficEngine()
			    .getStreetTypeById(id);

		    if (streetType == StreetSegment.TYPE_PRIMARY && z < 11)
			continue;
		    else if (streetType == StreetSegment.TYPE_SECONDARY
			    && z < 14)
			continue;
		    else if ((streetType == StreetSegment.TYPE_TERTIARY)
			    && z < 15)
			continue;
		    else if ((streetType == StreetSegment.TYPE_RESIDENTIAL)
			    && z < 17)
			continue;
		    else if (streetType == StreetSegment.TYPE_OTHER
			    || streetType == StreetSegment.TYPE_NON_ROADWAY)
			continue;

		    int colorNum;

		    SummaryStatistics baselineStats = TrafficEngineApp.engine
			    .getTrafficEngine().osmData.statsDataStore
			    .collectSummaryStatistics(id, normalizeByTime, w1,
				    hours);

		    if (baselineStats.getMean() > 0) {
			averageSpeed = baselineStats.getMean() * MS_TO_KMS;
			colorNum = (int) (10 / (50.0 / averageSpeed));
			if (colorNum > 10)
			    colorNum = 10;

			tile.renderLineString(TrafficEngineApp.engine
				.getTrafficEngine().getGeometryById(id),
				colors[colorNum], 2);
		    }

		} catch (MismatchedDimensionException | TransformException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }

	    try {
		return tile.generateImage();
	    } catch (IOException | ImageWriteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	    }
	}
    }

    public static class DataTile extends TrafficTileRequest {

	public DataTile(Integer x, Integer y, Integer z) {
	    super(x, y, z, "segment");

	}

	@Override
	public String getId() {
	    return super.getId();
	}

	@Override
	public byte[] render() {
	    GeometryFactory gf = new GeometryFactory();
	    Tile tile = new Tile(this);

	    List<SpatialDataItem> segments = TrafficEngineApp.engine
		    .getTrafficEngine().getOffMapTraces(tile.envelope);

	    Color pathColor = new Color(94 / 256.0f, 79 / 256.0f, 162 / 256.0f,
		    0.5f);

	    try {
		HashMap<String, Integer> traceCounts = new HashMap<>();

		for (SpatialDataItem sdi : segments) {
		    if (sdi.lats.length < 100)
			tile.renderLineString(sdi.getGeometry(), pathColor, 2);
		}

	    } catch (Exception e) {
		e.printStackTrace();
	    }

	    try {
		return tile.generateImage();
	    } catch (IOException | ImageWriteException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	    }
	}
    }
}
