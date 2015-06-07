package tiles;

import java.awt.Color;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.TransformException;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.ResultSetDelta;
import org.opentripplanner.analyst.ResultSetWithTimes;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;

import traffic.Engine;
import controllers.Application;

import com.conveyal.traffic.data.SpatialDataItem;
import com.conveyal.traffic.geom.StreetSegment;
import com.conveyal.traffic.osm.OSMDataStore;
import com.conveyal.traffic.stats.BaselineStatistics;
import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.strtree.STRtree;

public abstract class SimulatorTileRequest {
		
	final public String type;
	final public Integer x, y, z;
	
	public SimulatorTileRequest(Integer x, Integer y, Integer z, String type) {
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.type = type;
	}

	public String getId() {
		return type + "_" + x + "_" + y + "_" + z;
	}
	
	public boolean equals(SimulatorTileRequest tr) {
		return this.getId().equals(tr.getId());
	}
	
	public int hashCode() {
		HashFunction hashFunction = Hashing.md5();
        HashCode hashCode = hashFunction.newHasher().putString(this.getId(), Charsets.UTF_8).hash();
        
        return hashCode.asInt();
	}
	
	abstract byte[] render();
	
	
	public static class SegmentTile extends SimulatorTileRequest {
	
		public SegmentTile(Integer x, Integer y, Integer z) {
			super(x, y, z, "segment");
		
		}
		
		public String getId() {
			return super.getId();
		}
		
		public byte[] render(){
			GeometryFactory gf = new GeometryFactory();
			Tile tile = new Tile(this);
			
			HashSet<String> defaultEdges = new HashSet<String>();
 
			List<Envelope> envelopes  = Application.engine.getOsmEnvelopes();
			
    		List<SpatialDataItem> segments = Application.engine.getStreetSegments(tile.envelope);

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
    				else if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_TERTIARY && z < 16)
        				continue;
    				else if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_OTHER)
    					continue;
    				
    				int colorNum = 11;
    				
    				BaselineStatistics baselineStats = Application.engine.getTrafficEngine().getSegementStatistics(((StreetSegment)sdi).id);
    				
    				if(baselineStats.getAverageSpeedKMH() > 0) {
        				averageSpeed = baselineStats.getAverageSpeedKMH();
        				colorNum = (int) (10 / (50.0 / averageSpeed));
        				if(colorNum > 10)
        					colorNum = 10;
    				}
    				
    				Color color = colors[colorNum];
    				
    				tile.renderLineString(sdi.geometry, color, 2);
    			
					
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
	
	public static class DataTile extends SimulatorTileRequest {
		
		public DataTile(Integer x, Integer y, Integer z) {
			super(x, y, z, "segment");
		
		}
		
		public String getId() {
			return super.getId();
		}
		
		public byte[] render(){
			GeometryFactory gf = new GeometryFactory();
			Tile tile = new Tile(this);
			
			HashSet<String> defaultEdges = new HashSet<String>();
 
			List<Envelope> envelopes  = Application.engine.getOsmEnvelopes();
			
    		List<SpatialDataItem> segments = Application.engine.getStreetSegments(tile.envelope);

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
    		
			Color vehicleColor = new Color(158/256.0f,1/256.0f,66/256.0f,0.75f);
			
			for(Point p : Application.engine.getCurrentVehicleLocations()) {
				try {
					tile.renderPoint(p, 5,vehicleColor, 2);
				} catch (MismatchedDimensionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TransformException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
    		for(SpatialDataItem sdi : segments) {
    			
    			
    			try {
    				
    				int count = 0;
    				double averageSpeed = 0.0;
    			
    					
    				if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_PRIMARY && z < 11)
    					 continue;
    				else if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_SECONDARY && z < 14)
    					continue;
    				else if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_TERTIARY && z < 16)
        				continue;
    				else if(((StreetSegment)sdi).streetType == StreetSegment.TYPE_OTHER)
    					continue;
    				
    				int colorNum = 11;
    				
    				
    				
    				BaselineStatistics baselineStats = Application.engine.getTrafficEngine().getSegementStatistics(((StreetSegment)sdi).id);
    				if(baselineStats.getAverageSpeedKMH() > 0) {
    					colorNum = 5;
    				}
    				
    				Color color = colors[colorNum];
    				tile.renderLineString(sdi.geometry, color, 2);
    			
					
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
	
	
 
}
