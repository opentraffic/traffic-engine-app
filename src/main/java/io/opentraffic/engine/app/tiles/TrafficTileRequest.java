package io.opentraffic.engine.app.tiles;

import io.opentraffic.engine.app.data.WeeklyStatsObject;
import io.opentraffic.engine.data.stats.SummaryStatisticsComparison;
import io.opentraffic.engine.data.stores.SpatialDataStore;
import io.opentraffic.engine.geom.GPSPoint;
import io.opentraffic.engine.geom.StreetSegment;
import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.app.TrafficEngineApp;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import org.apache.commons.imaging.ImageWriteException;
import org.jcolorbrewer.ColorBrewer;
import org.mapdb.Fun;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public abstract class TrafficTileRequest {

    private static final int numberOfBins = Integer.parseInt(TrafficEngineApp.appProps.getProperty("application.numberOfBins"));
    private static final double maxSpeedInKph = Double.parseDouble(TrafficEngineApp.appProps.getProperty("application.maxSpeedInKph"));
    private static final Logger log = Logger.getLogger( TrafficTileRequest.class.getName());

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
	
	public int hashCode() {
		HashFunction hashFunction = Hashing.md5();
        HashCode hashCode = hashFunction.newHasher().putString(this.getId(), Charsets.UTF_8).hash();
        
        return hashCode.asInt();
	}
	
	abstract byte[] render();

	public static class SegmentTile extends TrafficTileRequest {
		Set<Integer> hours;
		Set<Integer> w1, w2;

		Boolean normalizeByTime;
		Integer confidenceInterval;

		public SegmentTile(Integer x, Integer y, Integer z, Boolean normalizeByTime, Integer confidenceInterval, List<Integer> w1, List<Integer> w2, List<Integer> hours) {
			super(x, y, z, "segment");

			this.hours = new HashSet<>(hours);
			this.w1 = new HashSet<>(w1);
			this.w2 = new HashSet<>(w2);
			this.normalizeByTime = normalizeByTime;
			this.confidenceInterval =confidenceInterval;
		}
		
		public String getId() {
			return super.getId();
		}

		public byte[] render() {
			if(this.w2 != null && this.w2.size() > 0)
				return renderPercentChange();
			else
				return renderSpeed();
		}

		public byte[] renderPercentChange() {

            int analysisLineWeight = Integer.parseInt(TrafficEngineApp.appProps.getProperty("application.analysisLineWeight"));

			Tile tile = new Tile(this);

    		List<Long> segmentIds = TrafficEngineApp.engine.getTrafficEngine().getStreetSegmentIds(tile.envelope);

			for(Long id : segmentIds) {

    			try {

    				double averageSpeed = 0.0;

					int streetType = TrafficEngineApp.engine.getTrafficEngine().getStreetTypeById(id);

    				if(streetType== StreetSegment.TYPE_PRIMARY && z < 11)
    					 continue;
    				else if(streetType == StreetSegment.TYPE_SECONDARY && z < 14)
    					continue;
    				else if((streetType == StreetSegment.TYPE_TERTIARY) && z < 15)
        				continue;
					else if((streetType == StreetSegment.TYPE_RESIDENTIAL) && z < 17)
						continue;
					else if(streetType == StreetSegment.TYPE_OTHER || streetType ==  StreetSegment.TYPE_NON_ROADWAY)
						continue;

					Color color;

    				SummaryStatistics stats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(id, normalizeByTime, w1, hours);
					SummaryStatistics stats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(id, normalizeByTime, w2, hours);

					SummaryStatisticsComparison statsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[confidenceInterval], stats1, stats2);

					Color[] colors;

					if(statsComparison.tTest()) {

						double speedPercentChange = statsComparison.differenceAsPercent();

						if(speedPercentChange < 0)
							colors = ColorBrewer.Reds.getColorPalette(5);
						else
							colors = ColorBrewer.Blues.getColorPalette(5);

						int colorNum;

						if(Math.abs(speedPercentChange) > .5)
							colorNum = 4;
						else {
							speedPercentChange = Math.abs(speedPercentChange) / 0.5 ;
							colorNum = (int)Math.round(4 * speedPercentChange);
						}

						tile.renderLineString(TrafficEngineApp.engine.getTrafficEngine().getGeometryById(id),  colors[colorNum], analysisLineWeight);

					}
					//else
					//	tile.renderLineString(TrafficEngineApp.engine.getTrafficEngine().getGeometryById(id),  Color.GREEN, 4);

				} catch (MismatchedDimensionException e) {
                    log.warning(e.getMessage());
					e.printStackTrace();
				} catch (TransformException e) {
                    log.warning(e.getMessage());
					e.printStackTrace();
				}
			}
    		
    		try {
				return tile.generateImage();
			} catch (IOException | ImageWriteException e) {
                log.warning(e.getMessage());
				e.printStackTrace();
				return null;
			}
		}

		public byte[] renderSpeed(){

            int analysisLineWeight = Integer.parseInt(TrafficEngineApp.appProps.getProperty("application.analysisLineWeight"));

			Tile tile = new Tile(this);

			List<Long> segmentIds = TrafficEngineApp.engine.getTrafficEngine().getStreetSegmentIds(tile.envelope);

			Color[] colors = ColorBrewer.RdYlBu.getColorPalette(numberOfBins + 1);

			for(Long id : segmentIds) {

				try {

					double averageSpeed = 0.0;

					int streetType = TrafficEngineApp.engine.getTrafficEngine().getStreetTypeById(id);

					if(streetType== StreetSegment.TYPE_PRIMARY && z < 11)
						continue;
					else if(streetType == StreetSegment.TYPE_SECONDARY && z < 14)
						continue;
					else if((streetType == StreetSegment.TYPE_TERTIARY) && z < 15)
						continue;
					else if((streetType == StreetSegment.TYPE_RESIDENTIAL) && z < 17)
						continue;
					else if(streetType == StreetSegment.TYPE_OTHER || streetType ==  StreetSegment.TYPE_NON_ROADWAY)
						continue;

					int colorNum;

					SummaryStatistics baselineStats = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(id,normalizeByTime, w1, hours);

					if(baselineStats.getMean() > 0) {
						averageSpeed = baselineStats.getMean() * WeeklyStatsObject.MS_TO_KMH;
                        colorNum = (int) (numberOfBins / (maxSpeedInKph / averageSpeed));
                        if(colorNum > numberOfBins)
                            colorNum = numberOfBins;
						tile.renderLineString(TrafficEngineApp.engine.getTrafficEngine().getGeometryById(id),  colors[colorNum], analysisLineWeight);
					}

				} catch (MismatchedDimensionException | TransformException e) {

					e.printStackTrace();
				}
			}

			try {
				return tile.generateImage();
			} catch (IOException | ImageWriteException e) {
				log.warning(e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public static class DataTile extends TrafficTileRequest {
		
		public DataTile(Integer x, Integer y, Integer z) {
			super(x, y, z, "data");
		
		}
		
		public String getId() {
			return super.getId();
		}
		
		public byte[] render()  {

			Tile tile = new Tile(this);

			GeometryFactory gf = new GeometryFactory();

			Envelope tileEnv = SpatialDataStore.tile2Envelope(x, y, z);

			int targetZ = z + 1;

			if(targetZ > 11)
				targetZ = 11;

			List<Fun.Tuple2<Integer, Integer>> tileIds = SpatialDataStore.getTilesForZ(x, y, z, targetZ);

			for(Fun.Tuple2<Integer, Integer> tileId : tileIds) {


				if(targetZ == 11) {

					Envelope env = SpatialDataStore.tile2Envelope(tileId.a, tileId.b, 11);

					Geometry tileGeom = gf.toGeometry(env);

					List<GPSPoint> points = TrafficEngineApp.engine.getTrafficEngine().vehicleState.getVehicleTilePoints(tileId);

					Color fillColor = new Color(64.0f / 255.0f, 224.0f / 255.0f, 208.0f / 255.0f, 0.5f);

					for(GPSPoint point : points) {
						try {
							tile.renderPoint(point.getPoint(), 3, fillColor, 2);
						} catch (MismatchedDimensionException e) {
							e.printStackTrace();
						} catch (TransformException e) {
							e.printStackTrace();
						}
					}

					fillColor = new Color(0.25f, 0.25f, 0.25f, 0.2f);
					try {
						tile.renderPolygon(tileGeom, new Color(0.5f, 0.5f, 0.5f, 0.2f), fillColor);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				else {

					Envelope env = SpatialDataStore.tile2Envelope(tileId.a, tileId.b, targetZ -1);

					Geometry tileGeom = gf.toGeometry(env);

					Color fillColor;

					Integer count = TrafficEngineApp.engine.getTrafficEngine().vehicleState.getVehicleTileCount(tileId);

					float alpha =  0.75f * ((float)count / (float)200);

					if(alpha > 0.75)
						alpha = 0.75f;

					if(count > 0)
						fillColor = new Color(64.0f / 255.0f, 224.0f / 255.0f, 208.0f / 255.0f, alpha);
					else
						fillColor = new Color(0.25f, 0.25f, 0.25f, 0.2f);

					try {
						tile.renderPolygon(tileGeom, new Color(0.5f, 0.5f, 0.5f, 0.2f), fillColor);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}

    		try {
				return tile.generateImage();
			} catch (IOException | ImageWriteException e ) {
                log.warning(e.getMessage());
				e.printStackTrace();
				return null;
			}
		} 
	}
}
