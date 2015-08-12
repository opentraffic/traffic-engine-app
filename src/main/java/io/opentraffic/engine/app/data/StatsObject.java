package io.opentraffic.engine.app.data;

import io.opentraffic.engine.app.TrafficEngineApp;

public class StatsObject{
	public Double locationsPerSecond;
	public Long locationsQueued;
	public Long locationsProcessed;
	public Long samplesProcessed;
	public Long samplesQueued;
	public Long vehicleCount;
	public Long lastUpdate;
	
	public StatsObject() {
		locationsPerSecond = TrafficEngineApp.engine.getTrafficEngine().getProcessingRate();
		locationsProcessed = TrafficEngineApp.engine.getTrafficEngine().getProcessedCount();
		locationsQueued = TrafficEngineApp.engine.getTrafficEngine().getQueueSize();
		samplesProcessed = TrafficEngineApp.engine.getTrafficEngine().getTotalSamplesProcessed();
		samplesQueued = TrafficEngineApp.engine.getTrafficEngine().getSampleQueueSize();

		vehicleCount = TrafficEngineApp.engine.getTrafficEngine().getVehicleCount();
	}
}