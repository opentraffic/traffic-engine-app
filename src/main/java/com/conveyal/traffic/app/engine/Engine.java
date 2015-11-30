package com.conveyal.traffic.app.engine;

import io.opentraffic.engine.TrafficEngine;
import io.opentraffic.engine.geom.GPSPoint;
import io.opentraffic.engine.osm.OSMArea;

import java.io.File;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.conveyal.traffic.app.TrafficEngineApp;
import com.vividsolutions.jts.geom.Point;

public class Engine {

    private static final Logger log = Logger.getLogger(Engine.class.getName());

    private TrafficEngine te;

    private HashMap<Long, Point> locationMap = new HashMap<>();

    public Engine() {

	String cacheDirectory = TrafficEngineApp.appProps
		.getProperty("application.data.cacheDirectory");
	String osmDirectory = TrafficEngineApp.appProps
		.getProperty("application.data.osmDirectory");
	String osmServer = TrafficEngineApp.appProps
		.getProperty("application.vex");

	Boolean enableTimeZoneConversion;
	try {
	    enableTimeZoneConversion = Boolean
		    .parseBoolean(TrafficEngineApp.appProps
			    .getProperty("application.enableTimeZoneConversion"));
	} catch (Exception e) {
	    enableTimeZoneConversion = true;
	    log.log(Level.INFO,
		    "Property enableTimeZoneConversion not set, defaulting to enabled");

	}

	Integer numberOfWorkerCores;
	try {
	    numberOfWorkerCores = Integer.parseInt(TrafficEngineApp.appProps
		    .getProperty("writeStatistics"));
	} catch (Exception e) {
	    numberOfWorkerCores = Runtime.getRuntime().availableProcessors() / 2;
	    log.log(Level.INFO,
		    "Property numberOfWorkerCores not set, defaulting to "
			    + numberOfWorkerCores + " cores.");

	}

	Integer osmCacheSize;
	try {
	    osmCacheSize = Integer.parseInt(TrafficEngineApp.appProps
		    .getProperty("application.osmCacheSize"));
	} catch (Exception e) {
	    log.log(Level.INFO,
		    "Property application.osmCacheSize not set, defaulting to cache size of 1,000,000");
	    osmCacheSize = 1_000_000;
	}

	te = new TrafficEngine(numberOfWorkerCores, new File(cacheDirectory),
		new File(osmDirectory), osmServer, osmCacheSize,
		enableTimeZoneConversion);

    }

    public TrafficEngine getTrafficEngine() {
	return te;
    }

    public void collectStatistics() {
	String trafficTilePath = TrafficEngineApp.appProps
		.getProperty("application.data.trafficTileDirectory");

	File dataCache = new File(trafficTilePath);
	dataCache.mkdirs();

	for (OSMArea osmArea : te.getOsmAreas()) {
	    te.writeStatistics(new File(dataCache, osmArea.z + "_" + osmArea.x
		    + "_" + osmArea.y + ".traffic.protobuf"), osmArea.env);
	}
    }

    public void locationUpdate(GPSPoint gpsPoint) {

	te.enqeueGPSPoint(gpsPoint);

    }

}
