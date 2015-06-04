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
	
	public EngineWorker(Engine te) {
		
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
				List<SpeedSample> speeds = te.update(gpsPoint);
				if(speeds != null) {
					for(SpeedSample speed : speeds) {
						if(!Engine.countMap.containsKey(speed.getSegmentId())) {
							Engine.countMap.put(speed.getSegmentId(), 0);
						}
						
						if(!Engine.speedMap.containsKey(speed.getSegmentId())) {
							Engine.speedMap.put(speed.getSegmentId(), 0.0);
						}
						
						Engine.countMap.put(speed.getSegmentId(), Engine.countMap.get(speed.getSegmentId()) + 1);
						Engine.speedMap.put(speed.getSegmentId(), Engine.speedMap.get(speed.getSegmentId()) + speed.getSpeed());
					}
				}
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
