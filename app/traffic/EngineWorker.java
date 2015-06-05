package traffic;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.conveyal.traffic.TrafficEngine;
import com.conveyal.traffic.geom.GPSPoint;
import com.conveyal.traffic.stats.SpeedSample;

public class EngineWorker implements Runnable {

	private ConcurrentLinkedQueue<GPSPoint> locationQueue = new ConcurrentLinkedQueue<GPSPoint>();
	
	private final Long id;
	
	private final Engine engine;
	
	public EngineWorker( Engine engine) {
		
		this.engine = engine;
		
		this.id = UUID.randomUUID().getLeastSignificantBits();
	
	}
	
	public Long getId() {
		return this.id;
	}
	
	public void enqueueLocationUpdate(GPSPoint gpsPoint) {
		locationQueue.add(gpsPoint);
	}
	
	@Override
	public void run() {
		
		// process queue location updates
		while(true) {
			
			GPSPoint gpsPoint = locationQueue.poll();
			
			if(gpsPoint != null) {
				int sampleCount = engine.getTrafficEngine().update(gpsPoint);
				
			} else {
				// if queue is empty pause
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// continue
				}
			}	
		}
	}

}
