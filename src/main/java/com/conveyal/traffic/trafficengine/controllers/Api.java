package com.conveyal.traffic.trafficengine.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.nio.ByteBuffer;

import org.geotools.geojson.geom.GeometryJSON;

import com.conveyal.traffic.data.ExchangeFormat;
import com.conveyal.traffic.geom.GPSPoint;
import com.conveyal.traffic.trafficengine.TrafficEngineApp;
import com.google.protobuf.InvalidProtocolBufferException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class Api {

    /*public static locationUpdate()  {
    	
    	try {
    		ExchangeFormat.VehicleMessage vm = ExchangeFormat.VehicleMessage.parseFrom(request().body().asRaw().asBytes());
    	
    		long vehicleId = getUniqueIdFromString(vm.getSourceId() + "_" + vm.getVehicleId());
    		
    		for(ExchangeFormat.VehicleLocation location : vm.getLocationsList()) {
    			
    			GPSPoint gpsPoint = new GPSPoint(location.getTimestamp(), vehicleId, location.getLon(), location.getLat());
    	
    			TrafficEngineApp.engine.locationUpdate(gpsPoint);
    		}
    	}
    	catch (Exception e) {
    		
    	}
    	
    }
    
    static Long getUniqueIdFromString(String data) throws NoSuchAlgorithmException {
    	
    	MessageDigest md = MessageDigest.getInstance("MD5");
		
		md.update(data.getBytes());
		ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / 8);  
		
		buffer.put(md.digest(), 0, Long.SIZE / 8);
        buffer.flip();//need flip 
        return buffer.getLong();
    	
    }*/
    
}
