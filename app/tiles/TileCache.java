package tiles;

import java.util.concurrent.ExecutionException;

import play.libs.Akka;
import play.libs.F.Function0;
import play.libs.F.Promise;
import scala.concurrent.ExecutionContext;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class TileCache extends CacheLoader<SimulatorTileRequest, byte[]> {
	
	 private LoadingCache<SimulatorTileRequest, byte[]> tileCache;

	 private int size = 200;
	 private int concurrency = 16;
	 
	 public TileCache() {
		 
		 this.tileCache = CacheBuilder.newBuilder()
				 .concurrencyLevel(concurrency)
				 .maximumSize(size)
				 .build(this);
	 }
	 
	 public byte[] get(SimulatorTileRequest req) {
		try {
			return tileCache.get(req);
		} catch (ExecutionException e) {
			return null;
		}
	 }

	 public void clear() {
		 tileCache.invalidateAll();
	 }
	 
	 @Override
	 public byte[] load(final SimulatorTileRequest request) throws Exception {
		 
		 return request.render();
	
	 }	 
}
