package traffic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import play.Logger;
import play.Play;

import com.conveyal.osmlib.OSM;
import com.conveyal.traffic.TrafficEngine;
import com.conveyal.traffic.data.SpatialDataItem;
import com.conveyal.traffic.geom.GPSPoint;
import com.google.common.io.ByteStreams;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;


public class Engine {
	
	private TrafficEngine te;
    
	ExecutorService executor;
	
	private HashMap<Long,Long> vehicleWorkerMap = new HashMap<Long,Long>();
	
	private HashMap<Long,EngineWorker> workerMap = new HashMap<Long,EngineWorker>();
	
	private ArrayList<Long> workerIdList = new ArrayList<Long>();
	private int nextWorkerIndex = 0;
	
    // map of loaded 
    private HashMap<String,Long> pbfFileIndex = new HashMap<String,Long>();
    
    private Quadtree osmBoundsIndex = new Quadtree();
    
    private long lastUpdate; 
    
    public Engine() {

    	File dataCache = new File("data/cache");
    	dataCache.mkdirs();
    	
    	te = new TrafficEngine(new File("data/cache"));
    	
    	executor = Executors.newFixedThreadPool(5);
    	
    	for (int i = 0; i < 5; i++) {
    		EngineWorker worker = new EngineWorker(this);
    		
    		workerMap.put(worker.getId(), worker);
    		
    		workerIdList.add(worker.getId());
    		
    		executor.execute(worker);
    	}

    }
    
    private Long getNextWorkerId() {
    	
    	Long workerId = workerIdList.get(nextWorkerIndex);	
    	
    	nextWorkerIndex++;
    	if(nextWorkerIndex >= workerIdList.size()) 
    		nextWorkerIndex = 0;
    	
    	return workerId;
    }
    
    public void locationUpdate(GPSPoint gpsPoint) {
    	
    	if(!vehicleWorkerMap.containsKey(gpsPoint.vehicleId))
    		vehicleWorkerMap.put(gpsPoint.vehicleId, getNextWorkerId());
    	
    	checkOsm(gpsPoint.lat, gpsPoint.lon);
    	GeometryFactory gf = new GeometryFactory();
    	
    	lastUpdate = gpsPoint.time;
    	
    	locationMap.put(gpsPoint.vehicleId, gf.createPoint(new Coordinate(gpsPoint.lon, gpsPoint.lat)));
    	
    	workerMap.get(vehicleWorkerMap.get(gpsPoint.vehicleId)).enqueueLocationUpdate(gpsPoint);
    	
    	
    }
    
    public List<SpatialDataItem> getStreetSegments(Envelope env) {
    	return te.getStreetSegments(env);
    }
    
    public long getLastUpdate() {
    	return lastUpdate;
    }
    
    public int getVehicleCount() {
    	return te.getVehicleCount();
    }
    
    public Collection<Point> getCurrentVehicleLocations() {
    	return locationMap.values();
    }
    
    public List<Envelope> getOsmEnvelopes() {
    	return (List<Envelope>)osmBoundsIndex.queryAll();
    }
    
    public void scanPbfDirectory() {
    	// enumerate PBF files in application.data.osmDirectory
    	File osmDirectory = new File(Play.application().configuration().getString("application.data.osmDirectory"));
    	
    	try {
			Files.walkFileTree(osmDirectory.toPath(), new FileVisitor<Path>() {
			   
			    // This method is called for each file visited. The basic attributes of the files are also available.
			    @Override
			    public FileVisitResult visitFile(Path file,
			            BasicFileAttributes attrs) throws IOException {
			        
			    	// check if file ends with .pbf
			    	if(file.toFile().getName().toLowerCase().endsWith(".osm.pbf")) {
						
			    		if(!pbfFileIndex.containsKey(file.toFile().getAbsolutePath())){
			    			Envelope env = loadPbfFile(file.toFile());    
				    		
			    			String[] pathParts = file.toFile().getAbsolutePath().split("\\/");
			    			String[] nameParts = file.toFile().getName().split("\\.");
			    			
			    			try {
			    				int z = Integer.parseInt(pathParts[pathParts.length-3]);
			    				int x = Integer.parseInt(pathParts[pathParts.length-2]);
			    				int y = Integer.parseInt(nameParts[0]);
			    				
			    				env = tile2Envelope(x, y, z);
			    				Logger.info("OSM file " + file.toFile().getName() + " for grid " + z + "/" + x + "/" + y + " (z/x/y)");
			    				
			    			}
			    			catch(Exception e) {
			    				Logger.info("OSM file " + file.toFile().getName() + " doesn't have tile gird info.");
			    			}
			    			
			    			if(env != null)
			    				osmBoundsIndex.insert(env, env);
			    			else 
			    				Logger.error("OSM file " + file.toFile().getName() + " is invalid");
			    		}
			    			
					}
					
					return FileVisitResult.CONTINUE;
			    }

				@Override
				public FileVisitResult preVisitDirectory(Path dir,
						BasicFileAttributes attrs) throws IOException {
					// TODO Auto-generated method stub
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc)
						throws IOException {
					// TODO Auto-generated method stub
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc)
						throws IOException {
					// TODO Auto-generated method stub
					return FileVisitResult.CONTINUE;
				}
			   
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public Envelope loadPbfFile(File pbfFile) {
    	
    	Logger.info("loading osm from: " + pbfFile.getAbsolutePath());
    	
    	// load pbf osm source and merge into traffic engine
		OSM osm = new OSM(null);
        osm.loadFromPBFFile(pbfFile.getAbsolutePath().toString());
        
        Envelope env = null;
        
        try {
        	// add OSM an truncate geometries
        	env = te.addOsm(osm,  false);
        	// add current OSM file and current timestamp to OSM pbf index.
        	pbfFileIndex.put(pbfFile.getAbsolutePath(), System.currentTimeMillis());   
        }
        catch (Exception e) { 
        	e.printStackTrace();
        	Logger.error("Unable to load osm: " + pbfFile.getAbsolutePath());
        }
   
        return env;
    }
    
    
    public void checkOsm(double lat, double lon) {
    	Envelope env1 = new Envelope();
    	env1.expandToInclude(lat, lon);
    	if(osmBoundsIndex.query(env1).size() == 0)
    		loadOSMTile(lat,lon, 11);
    }
    
    public void loadOSMTile(int x, int y, int z) {
    	
    	File osmDirectory = new File(Play.application().configuration().getString("application.data.osmDirectory"));
    	File zDir = new File(osmDirectory, "" + z);    	
    	File xDir = new File(zDir, "" + x);
    	File yPbfFile = new File(xDir, y + ".osm.pbf");
    	
    	if(!yPbfFile.exists()) {
    		xDir.mkdirs();
    		
    		Envelope env = tile2Envelope(x, y, z);
        	
    		Double south = env.getMinY() < env.getMaxY() ? env.getMinY() : env.getMaxY();
    		Double west = env.getMinX() < env.getMaxX() ? env.getMinX() : env.getMaxX();
    		Double north = env.getMinY() > env.getMaxY() ? env.getMinY() : env.getMaxY();
    		Double east = env.getMinX() > env.getMaxX() ? env.getMinX() : env.getMaxX();

    		String vexUrl = Play.application().configuration().getString("application.vex");

    		if (!vexUrl.endsWith("/"))
    			vexUrl += "/";

    		vexUrl += String.format("?n=%s&s=%s&e=%s&w=%s", north, south, east, west);

    		HttpURLConnection conn;
    		
    		Logger.info("loading osm from: " + vexUrl);
    		
			try {
				conn = (HttpURLConnection) new URL(vexUrl).openConnection();

	    		conn.connect();
	
	    		if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
	    			System.err.println("Received response code " +
	    					conn.getResponseCode() + " from vex server");
	
	    			return;
	    		}
	    		
	    		// download the file
	    		InputStream is = conn.getInputStream();
	    		OutputStream os = new FileOutputStream(yPbfFile);
	    		ByteStreams.copy(is, os);
	    		is.close();
	    		os.close();
	    		
	    		loadPbfFile(yPbfFile);
	    		
	    		osmBoundsIndex.insert(env, env);
	            
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
  	}
	
	public void loadOSMTile(double lat, double lon, int z) {
		int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<z) ) ;
        int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<z) ) ;
       
        loadOSMTile(xtile, ytile, z);
	}
	
	public static String getTileNumber(final double lat, final double lon, final int zoom) {
        int xtile = (int)Math.floor( (lon + 180) / 360 * (1<<zoom) ) ;
        int ytile = (int)Math.floor( (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1<<zoom) ) ;
        return("" + zoom + "/" + xtile + "/" + ytile);
    }


    public static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }
    
    public static double tile2lat(int y, int z) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
    
    public static Envelope tile2Envelope(final int x, final int y, final int zoom) {
        double maxLat = tile2lat(y, zoom);
        double minLat = tile2lat(y + 1, zoom);
        double minLon = tile2lon(x, zoom);
        double maxLon = tile2lon(x + 1, zoom);
        return new Envelope(minLon, maxLon, minLat, maxLat);
    }
}
