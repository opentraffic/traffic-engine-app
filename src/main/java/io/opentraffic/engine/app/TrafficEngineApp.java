package io.opentraffic.engine.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import io.opentraffic.engine.app.data.*;
import io.opentraffic.engine.app.engine.Engine;
import io.opentraffic.engine.app.routing.Routing;
import io.opentraffic.engine.app.tiles.TrafficTileRequest;
import io.opentraffic.engine.app.util.HibernateUtil;
import io.opentraffic.engine.app.util.PasswordUtil;
import io.opentraffic.engine.data.SpatialDataItem;
import io.opentraffic.engine.data.pbf.ExchangeFormat;
import io.opentraffic.engine.data.stats.SegmentStatistics;
import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.data.stats.SummaryStatisticsComparison;
import io.opentraffic.engine.geom.GPSPoint;
import io.opentraffic.engine.geom.StreetSegment;
import io.opentraffic.engine.osm.OSMCluster;
import org.apache.commons.cli.*;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jcolorbrewer.ColorBrewer;
import org.mapdb.Fun;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import spark.Request;
import spark.utils.StringUtils;

import javax.measure.Measure;
import javax.measure.unit.SI;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class TrafficEngineApp {

	private static final Logger log = Logger.getLogger( TrafficEngineApp.class.getName());
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	private static Routing routing = new Routing(new Rectangle(-180, -90, 360, 180));

    private static List<Envelope> importBoundingBoxes = null;
	
	public static Properties appProps = new Properties();
		
    public static Engine engine;

	public static HashMap<String,Long> vehicleIdMap = new HashMap<>();

	public static void main(String[] args) throws ParseException {

        Options options = new Options();
        options.addOption("c", true, "config file");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);

        String configFilePath = null;

        if(cmd.hasOption("c")){
            configFilePath = cmd.getOptionValue("c");
        }

		// load settings file
		loadSettings(configFilePath);
		
		// setup public folder
		staticFileLocation("/public");

		engine = new Engine();

		get("/writeTrafficTiles", (request, response) -> {
			engine.collectStatistics();
			return "Traffic tiles written.";
		});

		get("/cityNames", (request, response) -> {

            class City implements Comparable{
                public String city;
                public String country;
                public Double lat;
                public Double lon;

                @Override
                public int compareTo(Object o) {
                    return city.compareTo(((City)o).city);
                }
            };

			List<OSMCluster> clusters = engine.getTrafficEngine().osmData.getOSMClusters();
            Set<City> cityNames = new TreeSet<>();
            for(OSMCluster osmCluster : clusters){
                if(osmCluster != null){
                    if(osmCluster.name != null && osmCluster.name.indexOf(" -- ") > -1){
                        String countryName = osmCluster.name.split(" -- ")[0];
                        String cityName = osmCluster.name.split(" -- ")[1];
                        City city = new City();
                        city.city = cityName;
                        city.country = countryName;
                        city.lat = osmCluster.bounds.centre().y;
                        city.lon = osmCluster.bounds.centre().x;
                        cityNames.add(city);
                    }
                }
            }
			return mapper.writeValueAsString(cityNames);
		});

        get("/clusters", (request, response) -> {
            List<OSMCluster> clusters = engine.getTrafficEngine().osmData.getOSMClusters();
            return mapper.writeValueAsString(new ClusterList(clusters));
        });

		get("/stats", (request, response) -> new StatsObject(), mapper::writeValueAsString);

		get("/weeks", (request, response) ->  {

			List<Integer> weeks = engine.getTrafficEngine().osmData.statsDataStore.getWeekList();
			List<WeekObject> weekObjects = new ArrayList();
			for(Integer week : weeks) {
				WeekObject weekObj = new WeekObject();
				weekObj.weekId = week;
				weekObj.weekStartTime = SegmentStatistics.getTimeForWeek(week);
				weekObjects.add(weekObj);
			}
			return weekObjects;
		}, mapper::writeValueAsString);

        get("/colors", (request, response) ->  {
            int numberOfBins = Integer.parseInt(appProps.getProperty("application.numberOfBins"));
            double maxSpeedInKph = Double.parseDouble(TrafficEngineApp.appProps.getProperty("application.maxSpeedInKph"));
            Color[] colors = ColorBrewer.RdYlBu.getColorPalette(numberOfBins);
            String[] colorStrings = new String[numberOfBins];
            for(int i = 0; i < colors.length; i++){
                colorStrings[i] = "rgb(" + colors[i].getRed() + "," + colors[i].getGreen() + "," + colors[i].getBlue() + ")";
            }
            Map colorMap = new HashMap();
            colorMap.put("colorStrings", colorStrings);
            colorMap.put("maxSpeedInKph", maxSpeedInKph);
            return colorMap;
        }, mapper::writeValueAsString);

		get("/weeklyStats", (request, response) -> {

			response.header("Access-Control-Allow-Origin", "*");
			response.header("Access-Control-Request-Method", "*");
			response.header("Access-Control-Allow-Headers", "*");

			double x1 = request.queryMap("x1").doubleValue();
			double x2 = request.queryMap("x2").doubleValue();
			double y1 = request.queryMap("y1").doubleValue();
			double y2 = request.queryMap("y2").doubleValue();

			Set<Integer> weeks1 = new HashSet<>();
			Set<Integer> weeks2 = new HashSet<>();

			boolean normalizeByTime = request.queryMap("normalizeByTime").booleanValue();
			int confidenceInterval = request.queryMap("confidenceInterval").integerValue();

			if(request.queryMap("w1").value() != null && !request.queryMap("w1").value().trim().isEmpty()) {
				String valueStr[] = request.queryMap("w1").value().trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
				values.forEach(v -> weeks1.add(Integer.parseInt(v.trim())));
			}

			if(request.queryMap("w2").value() != null && !request.queryMap("w2").value().trim().isEmpty()) {
				String valueStr[] = request.queryMap("w2").value().trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
				values.forEach(v -> weeks2.add(Integer.parseInt(v.trim())));
			}

			Envelope env1 = new Envelope(x1, x2, y1, y2);

			Set<Long> segmentIds = TrafficEngineApp.engine.getTrafficEngine().getStreetSegmentIds(env1)
					.stream().collect(Collectors.toSet());

			SummaryStatistics summaryStats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(segmentIds,normalizeByTime, weeks1, null);

			if (weeks2.size() > 0) {
				SummaryStatistics summaryStats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(segmentIds,normalizeByTime, weeks2, null);
				SummaryStatisticsComparison summaryStatisticsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[confidenceInterval], summaryStats1, summaryStats2);

				return new WeeklyStatsObject(summaryStatisticsComparison);
			}
			else
				return new WeeklyStatsObject(summaryStats1);

		}, mapper::writeValueAsString);
		
		post("/locationUpdate", (request, response) -> {

            String importBoundingBoxesString = TrafficEngineApp.appProps.getProperty("application.importBoundingBoxes");
            if(!StringUtils.isEmpty(importBoundingBoxesString)){
                if(importBoundingBoxes == null){
                    importBoundingBoxes = new ArrayList<>();

                    String[] envelopeStrings = importBoundingBoxesString.split("\\|");
                    for(int i = 0; i < envelopeStrings.length; i++){
                        String[] envelopeCoordinates = envelopeStrings[i].split("\\,");
                        Double minLon = Math.min(Double.parseDouble(envelopeCoordinates[1]), Double.parseDouble(envelopeCoordinates[3]));
                        Double maxLon = Math.max(Double.parseDouble(envelopeCoordinates[1]), Double.parseDouble(envelopeCoordinates[3]));
                        Double minLat = Math.min(Double.parseDouble(envelopeCoordinates[0]), Double.parseDouble(envelopeCoordinates[2]));
                        Double maxLat = Math.max(Double.parseDouble(envelopeCoordinates[0]), Double.parseDouble(envelopeCoordinates[2]));
                        Envelope env = new Envelope(minLon,  maxLon, minLat, maxLat);
                        importBoundingBoxes.add(env);
                    }
                }
            }

			ExchangeFormat.VehicleMessageEnvelope vmEnvelope = ExchangeFormat.VehicleMessageEnvelope.parseFrom(request.bodyAsBytes());

			long sourceId = vmEnvelope.getSourceId();

			for(ExchangeFormat.VehicleMessage vm : vmEnvelope.getMessagesList()                ) {

				long vehicleId = getUniqueIdFromString(sourceId + "_" + vm.getVehicleId());

				for (ExchangeFormat.VehicleLocation location : vm.getLocationsList()) {

					GPSPoint gpsPoint = new GPSPoint(location.getTimestamp(), vehicleId, location.getLon(), location.getLat());

					if(gpsPoint.lat != 0.0 && gpsPoint.lon !=  0.0){
                        if(importBoundingBoxes != null){
                            for(Envelope env : importBoundingBoxes){
                                if(env.contains(gpsPoint.lon, gpsPoint.lat)){
                                    TrafficEngineApp.engine.locationUpdate(gpsPoint);
                                    break;
                                }
                            }
                        }else{
                            TrafficEngineApp.engine.locationUpdate(gpsPoint);
                        }
			        }

				}
			}
			return response;
		});
		
		// routing requests 
		
		get("/route", (request, response) -> {

            Integer missingStatsSearchEnvelopeInMeters = Integer.parseInt(appProps.getProperty("application.missingStatsSearchEnvelopeInMeters"));

            Integer missingStatsMaxSamples = Integer.parseInt(appProps.getProperty("application.missingStatsMaxSamples"));
			
			response.header("Access-Control-Allow-Origin", "*");

            Integer hourBin = request.queryMap("hour").integerValue();
			double fromLat = request.queryMap("fromLat").doubleValue();
			double fromLon = request.queryMap("fromLon").doubleValue(); 
			double toLat = request.queryMap("toLat").doubleValue(); 
			double toLon = request.queryMap("toLon").doubleValue();
            boolean compare = request.queryMap("compare").booleanValue();
            boolean normalizeByTime = request.queryMap("normalizeByTime").booleanValue();
            String confidenceInterval = request.queryMap("confidenceInterval").value();
			boolean useTraffic = true;


			Set<Integer> hours = new HashSet<>();

			if(request.queryMap("h").value() != null && !request.queryMap("h").value().trim().isEmpty()) {
				String valueStr[] = request.queryMap("h").value().trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
				values.forEach(v -> hours.add(Integer.parseInt(v.trim())));
			}

			Set<Integer> w1 = new HashSet<>();
			Set<Integer> w2 = new HashSet<>();

			if(request.queryMap("w1").value() != null && !request.queryMap("w1").value().trim().isEmpty()) {
				String valueStr[] = request.queryMap("w1").value().trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
				values.forEach(v -> w1.add(Integer.parseInt(v.trim())));
			}

			if(request.queryMap("w2").value() != null && !request.queryMap("w2").value().trim().isEmpty()) {
				String valueStr[] = request.queryMap("w2").value().trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
				values.forEach(v -> w2.add(Integer.parseInt(v.trim())));
			}

	        RoutingRequest rr = new RoutingRequest();

	        rr.useTraffic = useTraffic;
	        rr.from = new GenericLocation(fromLat, fromLon);
	        rr.to = new GenericLocation(toLat, toLon);
	        rr.modes = new TraverseModeSet(TraverseMode.CAR);

	        // figure out the time
	        LocalDateTime dt = LocalDateTime.now();
	        rr.dateTime = OffsetDateTime.of(dt, ZoneOffset.UTC).toEpochSecond();


            LocalDateTime twoHoursLater = dt.withHour(2);
            rr.dateTime = OffsetDateTime.of(twoHoursLater, ZoneOffset.UTC).toEpochSecond();

            while(!routing.isReady()){
                log.info("Graph not ready, waiting 1 second");
                Thread.sleep(1000);
            }

			List<Fun.Tuple3<Long, Long, Long>> edges = new ArrayList<>(routing.route(rr));

			TrafficPath trafficPath =new TrafficPath();

			Set<Long> edgeIds = new HashSet<>();

			Fun.Tuple3<Long, Long, Long> lastUnmatchedEdgeId = null;
			for(Fun.Tuple3<Long, Long, Long> edgeId : edges) {
				List<SpatialDataItem> streetSegments = engine.getTrafficEngine().getStreetSegmentsBySegmentId(edgeId);
				if(streetSegments.size() == 0) {
					if(lastUnmatchedEdgeId != null && lastUnmatchedEdgeId.a.equals(edgeId.a)) {
						edgeId = new Fun.Tuple3<>(edgeId.a, lastUnmatchedEdgeId.b, edgeId.c);
					}
					streetSegments = engine.getTrafficEngine().getStreetSegmentsBySegmentId(edgeId);
				}
				if(streetSegments.size() != 0) {
					for(SpatialDataItem sdi : streetSegments) {
						StreetSegment streetSegment = (StreetSegment)sdi;
						if(streetSegment != null) {
							lastUnmatchedEdgeId = null;
							edgeIds.add(streetSegment.id);
							SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, true, w1, hours);
							if(summaryStatistics != null){
                                if(summaryStatistics.count == 0){

                                    //no data for this segment, find nearby segments of the same road type and average those stats

                                    Envelope env = streetSegment.getGeometry().getEnvelopeInternal();
                                    Coordinate center = env.centre();
                                    final double mPerDegreeLat=111111.111111;
                                    double lat = center.y;
                                    double lonScale = Math.cos(Math.PI * lat / 180);
                                    double latExpand = missingStatsSearchEnvelopeInMeters / mPerDegreeLat;
                                    double lonExpand = latExpand / lonScale;
                                    env.expandBy(lonExpand,latExpand);
                                    List<SpatialDataItem> spatialDataItems = TrafficEngineApp.engine.getTrafficEngine().osmData.getStreetSegments(env);

                                    Map<Double, Long> distanceTosegmentIdMapMatchingType = new TreeMap<>(); //stats for same road type
                                    Map<Double, Long> distanceTosegmentIdMapDifferentType = new TreeMap<>(); //stats for any road type, fallback if no matching road types
                                    for(SpatialDataItem item : spatialDataItems) {
                                        if (item instanceof StreetSegment) {
                                            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
                                            com.vividsolutions.jts.geom.Point c1 = streetSegment.getGeometry().getCentroid();
                                            com.vividsolutions.jts.geom.Point c2 = item.getGeometry().getCentroid();
                                            Double distance = JTS.orthodromicDistance(new Coordinate(c1.getY(), c1.getX()), new Coordinate(c2.getY(), c2.getX()), sourceCRS);
                                            distance = Measure.valueOf(distance, SI.METER).doubleValue(SI.METER);
                                            SummaryStatistics nearbySummaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore
                                                    .collectSummaryStatistics(item.id, true, w1, hours);
                                            if(nearbySummaryStatistics.count > 0){
                                                if(distance > missingStatsSearchEnvelopeInMeters)
                                                    continue;

                                                StreetSegment nearbyStreetSegment = (StreetSegment) item;
                                                if(nearbyStreetSegment.streetType == streetSegment.streetType){
                                                    while(distanceTosegmentIdMapMatchingType.keySet().contains(distance)){
                                                        distance += .0000000001d;
                                                    }
                                                    distanceTosegmentIdMapMatchingType.put(distance, item.id);
                                                    if(distanceTosegmentIdMapMatchingType.keySet().size() == missingStatsMaxSamples)
                                                        break;
                                                }else{
                                                    while(distanceTosegmentIdMapDifferentType.keySet().contains(distance)){
                                                        distance += .0000000001d;
                                                    }
                                                    distanceTosegmentIdMapDifferentType.put(distance, item.id);
                                                }
                                            }
                                        }
                                    }

                                    Set<Long> segmentIds = new HashSet<>();
                                    if(distanceTosegmentIdMapMatchingType.keySet().size() > 0){
                                        segmentIds.addAll(distanceTosegmentIdMapMatchingType.values());
                                    }else{
                                        segmentIds.addAll(distanceTosegmentIdMapDifferentType.values());
                                    }

                                    summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(segmentIds, normalizeByTime, w1, hours);
                                    summaryStatistics.inferred = true;
                                }
                                if(compare){
                                    SummaryStatistics stats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w1, hours);
                                    SummaryStatistics stats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w2, hours);
                                    SummaryStatisticsComparison statsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[Integer.parseInt(confidenceInterval)], stats1, stats2);

                                    Color[] colors;

                                    if(statsComparison.tTest()) {
                                        double speedPercentChange = statsComparison.differenceAsPercent();
                                        if(speedPercentChange < 0)
                                            colors = ColorBrewer.Reds.getColorPalette(5);
                                        else
                                            colors = ColorBrewer.Blues.getColorPalette(5);
                                        int colorNum;

                                        if(Math.abs(speedPercentChange) > .5)
                                            colorNum = 4;
                                        else {
                                            speedPercentChange = Math.abs(speedPercentChange) / 0.5 ;
                                            colorNum = (int)Math.round(4 * speedPercentChange);
                                        }

                                        Color color = colors[colorNum];
                                        trafficPath.addSegment(streetSegment, summaryStatistics, color);
                                    }
                                }else{
                                    trafficPath.addSegment(streetSegment, summaryStatistics, null);
                                }
                            }
						}
						else {
							lastUnmatchedEdgeId = edgeId;
						}
					}
				}
				else {
					lastUnmatchedEdgeId = edgeId;
				}

			}

            SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(edgeIds, normalizeByTime, w1, null);
            if (w2.size() > 0) {
                SummaryStatistics summaryStats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(edgeIds,normalizeByTime, w2, null);
                SummaryStatisticsComparison summaryStatisticsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[Integer.parseInt(confidenceInterval)], summaryStatistics, summaryStats2);
                trafficPath.setWeeklyStats(new WeeklyStatsObject(summaryStatisticsComparison));
            }else{
                trafficPath.setWeeklyStats(summaryStatistics);
            }
            return mapper.writeValueAsString(trafficPath);
		});


		get("/tile/data", (request, response) -> {
			
			int x = request.queryMap("x").integerValue();
			int y = request.queryMap("y").integerValue();
			int z = request.queryMap("z").integerValue();
			
			response.raw().setHeader("CACHE_CONTROL", "no-cache, no-store, must-revalidate");
			response.raw().setHeader("PRAGMA", "no-cache");
			response.raw().setHeader("EXPIRES", "0");
			response.raw().setContentType("image/png");
			
			TrafficTileRequest.DataTile dataTile = new TrafficTileRequest.DataTile(x, y, z);
			
			byte[] imageData = dataTile.render();
			
			response.raw().getOutputStream().write(imageData);			
			return response;
		});
		
		get("/tile/traffic", (request, response) -> {

            int x = request.queryMap("x").integerValue();
            int y = request.queryMap("y").integerValue();
            int z = request.queryMap("z").integerValue();

            boolean normalizeByTime = request.queryMap("normalizeByTime").booleanValue();
            int confidenceInterval = request.queryMap("confidenceInterval").integerValue();

            List<Integer> hours = new ArrayList<>();

            if (request.queryMap("h").value() != null && !request.queryMap("h").value().trim().isEmpty()) {
                String valueStr[] = request.queryMap("h").value().trim().split(",");
                List<String> values = new ArrayList(Arrays.asList(valueStr));
                values.forEach(v -> hours.add(Integer.parseInt(v.trim())));
            }

            List<Integer> w1 = new ArrayList<>();
            List<Integer> w2 = new ArrayList<>();

            if (request.queryMap("w1").value() != null && !request.queryMap("w1").value().trim().isEmpty()) {
                String valueStr[] = request.queryMap("w1").value().trim().split(",");
                List<String> values = new ArrayList(Arrays.asList(valueStr));
                values.forEach(v -> w1.add(Integer.parseInt(v.trim())));
            }

            if (request.queryMap("w2").value() != null && !request.queryMap("w2").value().trim().isEmpty()) {
                String valueStr[] = request.queryMap("w2").value().trim().split(",");
                List<String> values = new ArrayList(Arrays.asList(valueStr));
                values.forEach(v -> w2.add(Integer.parseInt(v.trim())));
            }

            response.raw().setHeader("CACHE_CONTROL", "no-cache, no-store, must-revalidate");
            response.raw().setHeader("PRAGMA", "no-cache");
            response.raw().setHeader("EXPIRES", "0");
            response.raw().setContentType("image/png");

            TrafficTileRequest.SegmentTile dataTile = new TrafficTileRequest.SegmentTile(x, y, z, normalizeByTime, confidenceInterval, w1, w2, hours);

            byte[] imageData = dataTile.render();

            response.raw().getOutputStream().write(imageData);
            return response;
        });

        post("/login", (request, response) -> {
            Map<String, String> params = getPostParams(request);
            String username = params.get("username");
            String password = params.get("password");
            String cookie = params.get("token");
            if(password != null)
                password = PasswordUtil.hash(password);

            User user = HibernateUtil.login(username, password, cookie);

            if(user != null){
                return mapper.writeValueAsString(user);
            }
            response.status(403);
            return "Authentication failed";
        });

        post("/users", (request, response) -> {
            Map<String, String> params = getPostParams(request);
            String username = java.net.URLDecoder.decode(params.get("username"), "UTF-8");
            String password = params.get("password");
            String role = java.net.URLDecoder.decode(params.get("role"), "UTF-8");
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(PasswordUtil.hash(password));
            u.setRole(role);
            HibernateUtil.persistUser(u);
            response.status(200);
            return response;
        });

        put("/users/:id", (request, response) -> {
            Map<String, String> params = getPostParams(request);
            String username = java.net.URLDecoder.decode(params.get("username"), "UTF-8");
            String password = params.get("password");
            String role = java.net.URLDecoder.decode(params.get("role"), "UTF-8");
            Integer id = new Integer(request.params(":id"));

            User u = HibernateUtil.getUser(id);
            u.setUsername(username);
            if(password != null && !password.isEmpty()) {
              password = java.net.URLDecoder.decode(password, "UTF-8");
              u.setPasswordHash(PasswordUtil.hash(password));
            }
            u.setRole(role);
            HibernateUtil.updateUser(u);
            response.status(200);
            return mapper.writeValueAsString(u);
        });

        delete("/users/:id", (request, response) -> {
            Integer id = new Integer(request.params(":id"));
            HibernateUtil.deleteUser(id);
            response.status(200);
            return response;
        });

        get("/users", (request, response) -> {
            return mapper.writeValueAsString(HibernateUtil.getUsers());
        });

        routing.buildIfUnbuilt();

    }

    static Map<String, String> getPostParams(Request request){
        String body = request.body();
        String[] elements = body.split("\\&");
        Map<String, String> values = new HashMap<>();
        for(int i = 0; i < elements.length; i++){
            String[] kv = elements[i].split("\\=");
            if(kv.length == 2)
                values.put(kv[0], kv[1]);
        }
        return values;
    }

	
	static Long getUniqueIdFromString(String data) throws NoSuchAlgorithmException {

		if(vehicleIdMap.containsKey(data))
			return vehicleIdMap.get(data);

    	MessageDigest md = MessageDigest.getInstance("MD5");
		
		md.update(data.getBytes());
		ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE / 8);  
		
		buffer.put(md.digest(), 0, Long.SIZE / 8);
        buffer.flip();//need flip

		vehicleIdMap.put(data, buffer.getLong());
        return vehicleIdMap.get(data);

	}
	
	public static void loadSettings(String configFilePath) {
        if(configFilePath == null)
            configFilePath = "application.conf";
		try {
			FileInputStream in = new FileInputStream(configFilePath);
			appProps.load(in);
			in.close();
			
		} catch (IOException e) {
			log.log(Level.WARNING, "Unable to load application.conf file: {0}", e.getMessage());
			e.printStackTrace();
		}
	}



}
