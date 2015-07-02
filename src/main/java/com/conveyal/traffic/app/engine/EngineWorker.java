package com.conveyal.traffic.app.engine;

import com.conveyal.traffic.data.seralizers.GPSPointSeralizer;
import com.conveyal.traffic.geom.GPSPoint;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EngineWorker implements Runnable {
	
	private static final Logger log = Logger.getLogger( EngineWorker.class.getName() );

	private final Long id;

	private final Engine engine;

	private BlockingQueue<GPSPoint> locationQueue;

	private Long totalProcessed = 0l;
	private Long totalSamples = 0l;

	Atomic.Long queueSize;

	
	public EngineWorker(int workerId, Engine engine) {

		this.id = UUID.randomUUID().getLeastSignificantBits();
		
		this.engine = engine;

		DBMaker dbm = DBMaker.newFileDB(new File("queue_" + workerId + ".db"))
				.closeOnJvmShutdown()
				.transactionDisable()
				.asyncWriteEnable();

		DB db = dbm.make();

		if(db.getAtomicLong("queue_" + workerId + "_size") != null)
			this.queueSize = db.getAtomicLong("queue_" + workerId + "_size");
		else
			this.queueSize = db.createAtomicLong("queue_" + workerId + "_size", 0);

		if(db.getQueue("queue_" + workerId) != null)
			this.locationQueue = db.getQueue("queue_" + workerId);
		else
			this.locationQueue = db.createQueue("queue_" + workerId, new GPSPointSeralizer(), true);
	}
	
	public Long getId() {
		return this.id;
	}
	
	public void enqueueLocationUpdate(GPSPoint gpsPoint) {
		queueSize.incrementAndGet();
		locationQueue.add(gpsPoint);
	}

	public long getQueueSize() {
		return queueSize.get();
	}

	public long getTotalProcessed() {
		return totalProcessed;
	}

	public long getTotalSamples() {
		return totalSamples;
	}

	@Override
	public void run() {
		
		// process queue location updates
		while(true) {
			
			GPSPoint gpsPoint = locationQueue.poll();
			
			if(gpsPoint != null) {
				queueSize.decrementAndGet();
				try {
					totalSamples += engine.getTrafficEngine().update(gpsPoint);
					totalProcessed++;
				} catch (Exception e) {
					log.log(Level.WARNING, e.getMessage());
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
