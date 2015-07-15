package com.conveyal.traffic.app.engine;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.conveyal.traffic.TrafficEngine;
import com.conveyal.traffic.app.TrafficEngineApp;
import com.conveyal.traffic.data.SpatialDataItem;
import com.conveyal.traffic.geom.GPSPoint;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;


public class Engine {
	
	private static final Logger log = Logger.getLogger( Engine.class.getName());

	private TrafficEngine te;

	private HashMap<Long,Point> locationMap = new HashMap<>();

    
    public Engine() {

		String cacheDirectory =TrafficEngineApp.appProps.getProperty("application.data.cacheDirectory");
		String osmDirectory = TrafficEngineApp.appProps.getProperty("application.data.osmDirectory");
		String osmServer = TrafficEngineApp.appProps.getProperty("application.vex");

		Integer osmCacheSize;
		try {
			osmCacheSize = Integer.parseInt(TrafficEngineApp.appProps.getProperty("application.osmCacheSize"));
		} catch(Exception e) {
			log.log(Level.INFO, "Property application.osmCacheSize not set, defaulting to cache size of 1,000,000");
			osmCacheSize = 1_000_000;
		}

    	te = new TrafficEngine(new File(cacheDirectory), new File(osmDirectory), osmServer, osmCacheSize);

    }
    
    public TrafficEngine getTrafficEngine() { 
    	return te;
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

    	te.enqeueGPSPoint(gpsPoint);

    }
    
    public List<SpatialDataItem> getStreetSegments(Envelope env) {
    	return te.getStreetSegments(env);
    }


    public List<Envelope> getOsmEnvelopes() {
    	return (List<Envelope>)te.getOsmEnvelopes();
    }
}
