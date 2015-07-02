package com.conveyal.traffic.app.data;

import com.conveyal.traffic.app.TrafficEngineApp;

public class StatsObject{
	public Double locationsPerSecond;
	public Double samplesPerSecond;
	public Long locationsQueued;
	public Long locationsProcessed;
	public Long samplesProcessed;
	public Long samplesQueued;
	public Long vehicleCount;
	public Long lastUpdate;
	
	public StatsObject() {
		locationsPerSecond = TrafficEngineApp.engine.getLocationProcessingRate();
		samplesPerSecond = TrafficEngineApp.engine.getSampleProcessingRate();
		locationsProcessed = TrafficEngineApp.engine.getTotalLocationsProcessed();
		samplesProcessed = TrafficEngineApp.engine.getTotalSamplesProcessed();
		samplesQueued = TrafficEngineApp.engine.getSampleQueueSize();
		locationsQueued = TrafficEngineApp.engine.getQueueSize();
		vehicleCount = TrafficEngineApp.engine.getVehicleCount();
    	lastUpdate = TrafficEngineApp.engine.getLastUpdate();
	}
}