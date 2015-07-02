package com.conveyal.traffic.app.engine;

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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.conveyal.osmlib.OSM;
import com.conveyal.traffic.TrafficEngine;
import com.conveyal.traffic.app.TrafficEngineApp;
import com.conveyal.traffic.data.SpatialDataItem;
import com.conveyal.traffic.geom.GPSPoint;
import com.google.common.io.ByteStreams;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;


public class Engine {
	
	private static final Logger log = Logger.getLogger( Engine.class.getName());

	private TrafficEngine te;
    
	ExecutorService executor;
	
	private HashMap<Long,Long> vehicleWorkerMap = new HashMap<Long,Long>();
	
	private HashMap<Long,EngineWorker> workerMap = new HashMap<Long,EngineWorker>();
	
	public static ConcurrentHashMap<Long,Point> locationMap = new ConcurrentHashMap<Long,Point>();

	private ArrayList<Long> workerIdList = new ArrayList<Long>();
	private int nextWorkerIndex = 0;

    private long lastUpdate;

	private long lastStatsCheck;

	private long totalSamplesProcessedLastStatsCheck;
	private long totalUpdatesProcessedLastStatsCheck;
	private long totalSamplesWrittenLastStatsCheck;

	private double lastLocationProcessingRate;
	private double lastSampleProcessingRate;
    
    public Engine() {

		String cacheDirectory =TrafficEngineApp.appProps.getProperty("application.data.cacheDirectory");
		String osmDirectory = TrafficEngineApp.appProps.getProperty("application.data.osmDirectory");
		String osmServer = TrafficEngineApp.appProps.getProperty("application.vex");

    	te = new TrafficEngine(new File(cacheDirectory), new File(osmDirectory), osmServer);
    	
    	executor = Executors.newFixedThreadPool(5);
    	
    	for (int i = 0; i < 5; i++) {
    		EngineWorker worker = new EngineWorker(i, this);
    		
    		workerMap.put(worker.getId(), worker);
    		
    		workerIdList.add(worker.getId());
    		
    		executor.execute(worker);
    	}

    }
    
    public TrafficEngine getTrafficEngine() { 
    	return te;
    }
    
    private Long getNextWorkerId() {
    	
    	Long workerId = workerIdList.get(nextWorkerIndex);	
    	
    	nextWorkerIndex++;
    	if(nextWorkerIndex >= workerIdList.size()) 
    		nextWorkerIndex = 0;
    	
    	return workerId;
    }
    
    public void collectStatistics() {
    	File dataCache = new File("data/traffic");
    	dataCache.mkdirs();
    	
    	//for(String gridKey : gridEnvIndex.keySet()) {
    	//
    	//	te.writeStatistics(new File(dataCache, gridKey + ".traffic.protobuf"), gridEnvIndex.get(gridKey));
    	//}
    }
    
    public void locationUpdate(GPSPoint gpsPoint) {
    	if(!vehicleWorkerMap.containsKey(gpsPoint.vehicleId))
    		vehicleWorkerMap.put(gpsPoint.vehicleId, getNextWorkerId());

    	GeometryFactory gf = new GeometryFactory();
    	
    	lastUpdate = gpsPoint.time;
    	
    	locationMap.put(gpsPoint.vehicleId, gf.createPoint(new Coordinate(gpsPoint.lon, gpsPoint.lat)));
    	
    	workerMap.get(vehicleWorkerMap.get(gpsPoint.vehicleId)).enqueueLocationUpdate(gpsPoint);

		if(lastStatsCheck > 0) {
			long currentTime = System.currentTimeMillis();
			long delta = currentTime - lastStatsCheck;

			if(delta > 5000) {
				long totalLocationsProcessed = getTotalLocationsProcessed();
				long totalSamplesProcessed = getTotalSamplesProcessed();

				lastSampleProcessingRate = (double)(totalSamplesProcessed  - totalSamplesProcessedLastStatsCheck) / ((double)delta / 1000.0);
				lastLocationProcessingRate = (double)(totalLocationsProcessed  - totalUpdatesProcessedLastStatsCheck) / ((double)delta / 1000.0);

				totalSamplesProcessedLastStatsCheck = totalSamplesProcessed;
				totalUpdatesProcessedLastStatsCheck = totalLocationsProcessed;
				lastStatsCheck = currentTime;
			}
		}
		else
			lastStatsCheck = System.currentTimeMillis();
    }
    
    public List<SpatialDataItem> getStreetSegments(Envelope env) {
    	return te.getStreetSegments(env);
    }
    
    public long getLastUpdate() {
    	return lastUpdate;
    }
    
    public long getVehicleCount() {
    	return te.getVehicleCount();
    }

	public long getQueueSize() {
		Long queuedUpdates = 0l;
		for(EngineWorker worker : workerMap.values()) {
			queuedUpdates += worker.getQueueSize();
		}
		return queuedUpdates;
	}

	public long getTotalLocationsProcessed() {
		Long totalProcessed = 0l;
		for(EngineWorker worker : workerMap.values()) {
			totalProcessed += worker.getTotalProcessed();
		}
		return totalProcessed;
	}

	public long getTotalSamplesProcessed() {
		Long totalSamples = 0l;
		for(EngineWorker worker : workerMap.values()) {
			totalSamples += worker.getTotalSamples();
		}
		return totalSamples;
	}

	public long getSampleQueueSize() {
		return te.getSampleQueueSize();
	}

	public double getLocationProcessingRate() {
		return this.lastLocationProcessingRate;
	}
	public double getSampleProcessingRate() {
		return this.lastSampleProcessingRate;
	}

    public Collection<Point> getCurrentVehicleLocations() {
    	return locationMap.values();
    }
    
    public List<Envelope> getOsmEnvelopes() {
    	return (List<Envelope>)te.getOsmEnvelopes();
    }
}
