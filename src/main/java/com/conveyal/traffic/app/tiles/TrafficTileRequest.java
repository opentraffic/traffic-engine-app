package com.conveyal.traffic.app.tiles;

import com.conveyal.traffic.data.SpatialDataItem;
import com.conveyal.traffic.geom.OffMapTrace;
import com.conveyal.traffic.geom.StreetSegment;
import com.conveyal.traffic.stats.SegmentStatistics;
import com.conveyal.traffic.stats.SummaryStatistics;
import com.conveyal.traffic.app.TrafficEngineApp;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public abstract class TrafficTileRequest {
		
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
		Integer week, hour;
	
		public SegmentTile(Integer x, Integer y, Integer z, Integer week, Integer hour) {
			super(x, y, z, "segment");

			this.week = week;
			this.hour = null;
		}
		
		public String getId() {
			return super.getId();
		}
		
		public byte[] render(){

			GeometryFactory gf = new GeometryFactory();
			Tile tile = new Tile(this);
			
			HashSet<String> defaultEdges = new HashSet<String>();
 
			List<Envelope> envelopes  = TrafficEngineApp.engine.getOsmEnvelopes();
			
    		List<SpatialDataItem> segments = TrafficEngineApp.engine.getStreetSegments(tile.envelope);

    		Color[] colors = new Color[12];
			
			colors[0] = new Color(158/256.0f,1/256.0f,66/256.0f,0.5f);
			colors[1] = new Color(213/256.0f,62/256.0f,79/256.0f,0.5f);
			colors[2] = new Color(244/256.0f,109/256.0f,67/256.0f,0.5f);
			colors[3] = new Color(253/256.0f,174/256.0f,97/256.0f,0.5f);
			colors[4] = new Color(254/256.0f,224/256.0f,139/256.0f,0.5f);
			colors[5] = new Color(255/256.0f,255/256.0f,191/256.0f,0.5f);
			colors[6] = new Color(230/256.0f,245/256.0f,152/256.0f,0.5f);
			colors[7] = new Color(171/256.0f,221/256.0f,164/256.0f,0.5f);
			colors[8] = new Color(102/256.0f,194/256.0f,165/256.0f,0.5f);
			colors[9] = new Color(50/256.0f,136/256.0f,189/256.0f,0.5f);
			colors[10] = new Color(94/256.0f,79/256.0f,162/256.0f,0.5f);
			colors[11] = new Color(96/256.0f,96/256.0f,96/256.0f,0.5f);
    		
			
    		for(SpatialDataItem sdi : segments) {
    			
    			
    			try {
    				
    				int count = 0;
    				double averageSpeed = 0.0;
    			
    					
    				if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_PRIMARY && z < 11)
    					 continue;
    				else if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_SECONDARY && z < 14)
    					continue;
    				else if((((StreetSegment)sdi).streetType == StreetSegment.TYPE_TERTIARY  || ((StreetSegment)sdi).streetType == StreetSegment.TYPE_RESIDENTIAL) && z < 16)
        				continue;
					else if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_OTHER)
						continue;


					int colorNum = 11;

					Color color;

    				SummaryStatistics baselineStats = TrafficEngineApp.engine.getTrafficEngine().collectSummaryStatisics(sdi.id, null);
					if(hour != null) {
						//SummaryStatistics weekStats = TrafficEngineApp.engine.getTrafficEngine().collectSummaryStatisics(sdi.id, week);

						averageSpeed = baselineStats.getAverageSpeedKMH();
						double hourSpeed = baselineStats.getSpeedByHourOfWeekKMH(hour);
						if(!Double.isNaN(hourSpeed)) {
							double speedPercentChange = (averageSpeed - hourSpeed) / averageSpeed;

							if(speedPercentChange > .25)
								colorNum = 10;
							else if(speedPercentChange < -.25)
								colorNum = 0;
							else {
								speedPercentChange = 0.25 / speedPercentChange;
								colorNum = 5 + (int)Math.round(5 / speedPercentChange);
							}
						}
					}
					else {
						if(baselineStats.getAverageSpeedKMH() > 0) {
							averageSpeed = baselineStats.getAverageSpeedKMH();
							colorNum = (int) (10 / (50.0 / averageSpeed));
							if(colorNum > 10)
								colorNum = 10;
						}

					}

					color = colors[colorNum];

					if(colorNum == 1)
						tile.renderLineString(sdi.getGeometry(), color, 2);

					else

    				tile.renderLineString(sdi.getGeometry(), color, 2);
					
				} catch (MismatchedDimensionException | TransformException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    		
    		try {
				return tile.generateImage();
			} catch (IOException e) {
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
		
		public String getId() {
			return super.getId();
		}
		
		public byte[] render(){
			GeometryFactory gf = new GeometryFactory();
			Tile tile = new Tile(this);
			
    		List<SpatialDataItem> segments = TrafficEngineApp.engine.getTrafficEngine().getOffMapTraces(tile.envelope);

    		Color pathColor = new Color(94/256.0f,79/256.0f,162/256.0f,0.5f);
			Color pointColor = new Color(200/256.0f,20/256.0f,20/256.0f,0.5f);

			try {
				HashMap<String, Integer> traceCounts = new HashMap<>();

				for (SpatialDataItem sdi : segments) {
					if(sdi.lats.length < 100)
						tile.renderLineString(sdi.getGeometry(), pathColor, 2);
				}

			} catch (Exception e){
				e.printStackTrace();
			}
    		
    		try {
				return tile.generateImage();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
			
		} 
	} 
	
	
}
