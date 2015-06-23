package com.conveyal.traffic.app.controllers;

import com.conveyal.traffic.app.TrafficEngineApp;

public class StatsObject{
	public Integer vehicleCount;
	public Long lastUpdate;
	
	public StatsObject() {
		vehicleCount = TrafficEngineApp.engine.getVehicleCount();
    	lastUpdate = TrafficEngineApp.engine.getLastUpdate();
	}
}