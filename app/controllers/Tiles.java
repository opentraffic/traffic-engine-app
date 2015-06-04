package controllers;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opentripplanner.analyst.ResultSetDelta;
import org.opentripplanner.analyst.ResultSetWithTimes;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.api.model.TimeSurfaceShort;
import org.opentripplanner.common.model.GenericLocation;

import com.vividsolutions.jts.index.strtree.STRtree;

import play.libs.Akka;
import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Function0;
import play.libs.F.Promise;
import play.mvc.*;
import scala.concurrent.ExecutionContext;
import tiles.SimulatorTileRequest;
import tiles.SimulatorTileRequest.SegmentTile;
import tiles.SimulatorTileRequest.DataTile;
import tiles.Tile;

import tiles.TileCache;

public class Tiles extends Controller {

	private static TileCache tileCache = new TileCache();

	public static void resetTileCache() {
		tileCache.clear();
	}

	public static Promise<Result> tileBuilder(final SimulatorTileRequest tileRequest) {

		response().setHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
		response().setHeader(PRAGMA, "no-cache");
		response().setHeader(EXPIRES, "0");
		response().setContentType("image/png");


		Promise<byte[]> promise = Promise.promise(
		    new Function0<byte[]>() {
		      public byte[] apply() {
		    	  return tileCache.get(tileRequest);
		      }
		    }
		  );
		Promise<Result> promiseOfResult = promise.map(
		    new Function<byte[], Result>() {
		      public Result apply(byte[] response) {

		    	if(response == null)
		    	  return notFound();

		    	ByteArrayInputStream bais = new ByteArrayInputStream(response);

				return ok(bais);
		      }
		    }
		  );

		return promiseOfResult;
	}


	public static Promise<Result> segment(Integer x, Integer y, Integer z) {

		SimulatorTileRequest tileRequest = new SegmentTile(x, y, z);
		return tileBuilder(tileRequest);
    }
	
	public static Promise<Result> data(Integer x, Integer y, Integer z) {

		SimulatorTileRequest tileRequest = new DataTile(x, y, z);
		return tileBuilder(tileRequest);
    }

}
