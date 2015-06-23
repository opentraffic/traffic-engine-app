package com.conveyal.traffic.app.tiles;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.analyst.request.TileRequest;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class Tile {
	
	final public String id;
	final public Integer x, y, z;
	
	final public Integer scaleFactor;
	final private MathTransform tr;
	
	public BufferedImage buffer;	
	public Graphics2D gr;
	
	final public Envelope envelope; 
	
	public Tile(TrafficTileRequest req) {
		
		this.x = req.x;
		this.y = req.y;
		this.z = req.z;
		
		if(14 - z <= 0)
			this.scaleFactor = 1;
		else
			this.scaleFactor = 14 - z;
		
		this.id = req.getId();
		
		double maxLat = SlippyTile.tile2lat(y, z);
        double minLat = SlippyTile.tile2lat(y + 1, z);
        double minLon = SlippyTile.tile2lon(x, z);
        double maxLon = SlippyTile.tile2lon(x + 1, z);
    	
        // annoyingly need both jts and opengis envelopes -- there's probably a smarter way to get them
    	envelope = new Envelope(maxLon, minLon, maxLat, minLat);
         
    	Envelope2D env = JTS.getEnvelope2D(envelope, DefaultGeographicCRS.WGS84);
    	
    	TileRequest tileRequest = new TileRequest(env, 256 * this.scaleFactor, 256 * this.scaleFactor);
    	GridEnvelope2D gridEnv = new GridEnvelope2D(0, 0, tileRequest.width, tileRequest.height);
    	GridGeometry2D gg = new GridGeometry2D(gridEnv, (org.opengis.geometry.Envelope)(tileRequest.bbox));
    	
      	tr = gg.getCRSToGrid2D();
      	
      	buffer = new BufferedImage(tileRequest.width, tileRequest.height, BufferedImage.TYPE_4BYTE_ABGR); 
       
	} 
	
	
	public void renderPolygon(Geometry g, Color c, Color stroke) throws MismatchedDimensionException, TransformException {
		
		if(g instanceof com.vividsolutions.jts.geom.MultiPolygon) {
			com.vividsolutions.jts.geom.MultiPolygon gM = ((com.vividsolutions.jts.geom.MultiPolygon)g);
			for(int nGm = 0; nGm < gM.getNumGeometries(); nGm++) {
				renderPolygon(gM.getGeometryN(nGm), c, stroke);	
			}
		}
		else {
			if(gr == null)
				gr = buffer.createGraphics();
			
			gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
	                RenderingHints.VALUE_ANTIALIAS_ON);
			
			Geometry gTr  = JTS.transform(g, tr);
	        
			Coordinate[] coords;
			
			if(gTr instanceof com.vividsolutions.jts.geom.Polygon) {
				com.vividsolutions.jts.geom.Polygon pTr = (com.vividsolutions.jts.geom.Polygon)gTr;
				coords = pTr.getExteriorRing().getCoordinates();
			}
			else
				coords = gTr.getCoordinates();
			
			gr.setColor(c);
			
			if(coords.length > 1) {
				
				Polygon p = new Polygon();
		    	for(Coordinate coord : coords)
		    		p.addPoint((int)coord.x, (int)coord.y);
		    	
		    	gr.fillPolygon(p);     
		    	
		    	if(stroke != null) {
		    		gr.setColor(stroke);
		    		gr.setStroke(new BasicStroke(2));
		    		gr.drawPolygon(p);
		    	}
		    	
		    	if(gTr instanceof com.vividsolutions.jts.geom.Polygon) {
					com.vividsolutions.jts.geom.Polygon pTr = (com.vividsolutions.jts.geom.Polygon)gTr;
					
					Color hColor = new Color(1.0f,1.0f,1.0f,0.0f);
					
					for(int nIr = 0; nIr < pTr.getNumInteriorRing(); nIr++) {
						coords = pTr.getInteriorRingN(nIr).getCoordinates();
						
						
						p = new Polygon();
				    	for(Coordinate coord : coords)
				    		p.addPoint((int)coord.x, (int)coord.y);
				    	gr.setComposite(AlphaComposite.Clear);
				    	gr.setColor(hColor);
				    	gr.fillPolygon(p);     
				    	
				    	gr.setComposite(AlphaComposite.SrcOver);
				    	
				    	if(stroke != null) {
				    		gr.setColor(stroke);
				    		gr.setStroke(new BasicStroke(2));
				    		gr.drawPolygon(p);
				    	}
					}
				}
			}
			else {
				gr.fillOval((int)coords[0].x, (int)coords[0].y, 5, 5);
			}
		}
	}
	
	public void renderPoint(Point pt, double radius, Color c, Integer strokeWidth) throws MismatchedDimensionException, TransformException {
		
		if(gr == null)
			gr = buffer.createGraphics();
		
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		
		Geometry gTr  = JTS.transform(pt, tr);
        
		gr.setColor(c);
		
		if(strokeWidth != null)
			gr.setStroke(new BasicStroke(5));
		
		Shape circle = new Ellipse2D.Double(gTr.getCoordinate().x - radius, gTr.getCoordinate().y - radius, 2.0 * radius, 2.0 * radius);
		
    	gr.draw(circle);    
	}
	
	public void renderLineString(Geometry g, Color c, Integer strokeWidth) throws MismatchedDimensionException, TransformException {
		
		if(gr == null)
			gr = buffer.createGraphics();
		
		gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		
		Geometry gTr  = JTS.transform(g, tr);
        
		gr.setColor(c);
		
		if(strokeWidth != null)
			gr.setStroke(new BasicStroke(strokeWidth));
		
		Path2D path = new Path2D.Double();
		
		boolean firstPoint = true;
    	for(Coordinate coord : gTr.getCoordinates()) {
    		if(firstPoint)
    			path.moveTo(coord.x, coord.y);
    		else
    			path.lineTo(coord.x, coord.y);
    		
    		firstPoint = false;
    	}
    	
    	gr.draw(path);    
	}
	
	public byte[] generateImage() throws IOException {
		
		if(this.scaleFactor > 1) {
			
			int w = buffer.getWidth();
            int h = buffer.getHeight();
			
			do {
				w /= 2;
                if (w < 256) {
                    w = 256;
                }
                
                h /= 2;
                if (h < 256) {
                    h = 256;
                }
				
				BufferedImage original = buffer;
				

				BufferedImage resized = new BufferedImage(w, h, original.getType());
			    Graphics2D g = resized.createGraphics();
			    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			    g.drawImage(original, 0, 0, w, h, null);
			    g.dispose();
			    g = null;
			    
			    buffer = resized;

	        } while (w != 256 || h != 256);
		}
		
		if(gr != null)
			gr.dispose();
		gr = null;
		
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffer, "png", baos);
        
        return baos.toByteArray(); 
	} 
}
