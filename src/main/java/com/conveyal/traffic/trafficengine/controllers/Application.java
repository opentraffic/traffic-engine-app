package com.conveyal.traffic.trafficengine.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.geojson.geom.GeometryJSON;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import static spark.Spark.*;

public class Application {


    
  /*  public static Result osmEnvelopes() throws IOException {
        // grap list of envelopes and generate geoJSON
        List<Envelope> envelopes = engine.getOsmEnvelopes();
        
        GeometryFactory gf = new GeometryFactory();
        
        ArrayList<Polygon> polygons = new ArrayList<Polygon>();
        for(Envelope e : envelopes) {
        	Geometry eGeom = gf.toGeometry(e);
        	
        	if(eGeom instanceof Polygon)
        		polygons.add((Polygon)eGeom);
        }
        
        Polygon[] poly = new Polygon[polygons.size()];
        MultiPolygon gc = new MultiPolygon(polygons.toArray(poly), gf);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GeometryJSON gjson = new GeometryJSON();
        gjson.writeGeometryCollection(gc, out);
        
        String geoJsonStr = new String( out.toByteArray() );
        return ok(geoJsonStr);
    }
    
    public static Result vehiclePoints() throws IOException {
        // grap list of envelopes and generate geoJSON
        List<Point> points = new ArrayList<Point>();//engine.te.getVehiclePoints();
        
        GeometryFactory gf = new GeometryFactory();
        
        
        Point[] poly = new Point[points.size()];
        MultiPoint gc = new MultiPoint(points.toArray(poly), gf);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GeometryJSON gjson = new GeometryJSON();
        gjson.writeGeometryCollection(gc, out);
        
        String geoJsonStr = new String( out.toByteArray() );
        return ok(geoJsonStr);
    }

    public static Result collectStatistics() throws IOException {
        Application.engine.collectStatistics();
        return ok();
    }
    
    public static Result triplines() {
        Envelope env = new Envelope();
//        env.expandToInclude(x1, y1);
//        env.expandToInclude(x2, y2);

       // engine.te.getTripLines(env);



        return ok();
    } */
}
