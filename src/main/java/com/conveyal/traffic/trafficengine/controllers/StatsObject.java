package com.conveyal.traffic.trafficengine.controllers;

import com.conveyal.traffic.trafficengine.TrafficEngineApp;

public class StatsObject{
	public Integer vehicleCount;
	public Long lastUpdate;
	
	public StatsObject() {
		vehicleCount = TrafficEngineApp.engine.getVehicleCount();
    	lastUpdate = TrafficEngineApp.engine.getLastUpdate();
	}
}