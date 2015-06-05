package traffic;

import com.conveyal.traffic.geom.GPSPoint;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

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
				try {
					int sampleCount = engine.getTrafficEngine().update(gpsPoint);
				} catch (Exception e) {
					e.printStackTrace();
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
