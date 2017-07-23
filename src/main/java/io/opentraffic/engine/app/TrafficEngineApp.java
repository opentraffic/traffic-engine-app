package io.opentraffic.engine.app;

import com.carrotsearch.hppc.cursors.IntCursor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.MediaType;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import io.opentraffic.engine.app.data.*;
import io.opentraffic.engine.app.engine.Engine;
import io.opentraffic.engine.app.routing.Routing;
import io.opentraffic.engine.app.tiles.TrafficTileRequest;
import io.opentraffic.engine.app.util.ShapefileUtil;
import io.opentraffic.engine.data.SpatialDataItem;
import io.opentraffic.engine.data.pbf.ExchangeFormat;
import io.opentraffic.engine.data.stats.SegmentStatistics;
import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.data.stats.SummaryStatisticsComparison;
import io.opentraffic.engine.geom.GPSPoint;
import io.opentraffic.engine.geom.StreetSegment;
import io.opentraffic.engine.osm.OSMCluster;
import org.apache.commons.cli.*;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jcolorbrewer.ColorBrewer;
import org.mapdb.Fun;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.zeroturnaround.zip.ZipUtil;
import spark.Request;
import spark.utils.StringUtils;


import javax.measure.Measure;
import javax.measure.unit.SI;
import javax.servlet.ServletOutputStream;
import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class TrafficEngineApp {

	private static final Logger log = Logger.getLogger( TrafficEngineApp.class.getName());
	
	private static final ObjectMapper mapper = new ObjectMapper();
	
	private static Routing routing = new Routing(new Rectangle(-180, -90, 360, 180));;

    private static List<Envelope> importBoundingBoxes = null;
	
	public static Properties appProps = new Properties();
		
    public static Engine engine;

	public static HashMap<String,Long> vehicleIdMap = new HashMap<>();


    public static String renderContent(String htmlFile) {
        try {
            // If you are using maven then your files
            // will be in a folder called resources.
            // getResource() gets that folder
            // and any files you specify.
            InputStream inputStream = TrafficEngineApp.class.getClassLoader().getResourceAsStream(htmlFile);

            BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
            return buffer.lines().collect(Collectors.joining("\n"));

        } catch (Exception e) {
            // Add your own exception handlers here.
        }
        return null;
    }

    public static void initRouting () {

        // lazy loading OTP
        synchronized (routing) {
            if(routing == null)
                routing = new Routing(new Rectangle(-180, -90, 360, 180));
        }

        // check if graph is ready
        while(!routing.isReady()){
            log.info("Graph not ready, waiting 1 second");

            try {
                Thread.sleep(1000);
            }
            catch (Exception e){
                // noop
            }
        }

    }

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

        String portString = appProps.getProperty("application.port");
        if(!StringUtils.isEmpty(portString)){
            port(Integer.parseInt(portString));
        }

        before((request, response) -> {
            String pathInfo = request.pathInfo();
            log.info(pathInfo);

            if(pathInfo.startsWith("/stylesheets/") || pathInfo.startsWith("/images/") || pathInfo.startsWith("/javascripts/") || pathInfo.startsWith("/locales/") || pathInfo.startsWith("/templates/"))
                return;

            boolean authenticated = false;

            //  check if authenticated
            if(request.session().attribute("user") != null)
                authenticated = true;

            if(!authenticated && !pathInfo.startsWith("/auth")) {
                response.redirect("/auth");
                halt();
            }

        });

        get("/", (request, response) -> {
            return renderContent("templates/index.html");
        });

        get("/auth", (request, response) -> {

            return renderContent("templates/login.html");
        });

        post("/auth", (request, response) -> {

            String action = request.queryParams("action");

            if(action == null)
                halt(401);
            else if(action.equals("login")){

                String username = request.queryParams("username");
                String password = request.queryParams("password");

                if(     (username.toLowerCase().trim().equals("demo") && password.toLowerCase().trim().equals("phdata")) ||
                        (username.toLowerCase().trim().equals("admin") && password.toLowerCase().trim().equals("admin"))) {
                    request.session(true);
                    request.session().attribute("user", username);

                    return response;
                }
                else
                    halt(401);
            }
            else if(action.equals("logout")){
                request.session().removeAttribute("user");
                response.redirect("/auth");
                return response;
            }
            else {
                halt(401);
            }

            return response;
        });

        get("/download", (request, response) -> {

            String filename = request.queryMap("filename").value();
            File file = new File(filename);

            response.header("Content-Disposition", String.format("attachment; filename=\"%s\"", filename));
            response.type(MediaType.OCTET_STREAM.toString());
            response.raw().setContentLength((int) file.length());
            response.status(200);

            final ServletOutputStream os = response.raw().getOutputStream();
            final FileInputStream in = new FileInputStream(file);
            IOUtils.copy(in, os);
            in.close();
            os.close();
            file.delete();
            return null;
        });

        post("/csv", (request, response) -> {

            class StatsVO {
                public Long edgeId;
                public SummaryStatistics summaryStatistics;
                public SummaryStatisticsComparison summaryStatisticsComparison;
                public SummaryStatistics summaryStatisticsCompare1;
                public SummaryStatistics summaryStatisticsCompare2;
                public StreetSegment streetSegment;
            }

            List<StatsVO> statsVOs = new ArrayList<>();

            response.header("Access-Control-Allow-Origin", "*");

            Map<String, Object> paramMap= mapper.readValue(request.body(), new TypeReference<Map<String, Object>>(){});

            List<Integer> hours = new ArrayList<>();

            Integer utcAdjustment = (Integer)paramMap.get("utcAdjustment");

            if(paramMap.containsKey("h") && !((String)paramMap.get("h")).trim().isEmpty()) {
                String valueStr[] = ((String)paramMap.get("h")).trim().split(",");
                List<String> values = new ArrayList(Arrays.asList(valueStr));
                for(String value : values){
                    int uncorrectedHour = new Integer(value);
                    int utcCorrectedHour = fixIncomingHour(uncorrectedHour, utcAdjustment);
                    hours.add(utcCorrectedHour);
                }
            }

            Set<Integer> w1 = new HashSet<>();
            Set<Integer> w2 = new HashSet<>();

            if(paramMap.containsKey("w1") && !((String)paramMap.get("w1")).trim().isEmpty()) {
                String valueStr[] = ((String)paramMap.get("w1")).trim().split(",");
                List<String> values = new ArrayList(Arrays.asList(valueStr));
                values.forEach(v -> w1.add(Integer.parseInt(v.trim())));
            }

            if(paramMap.containsKey("w2") && !((String)paramMap.get("w2")).trim().isEmpty()) {
                String valueStr[] = ((String)paramMap.get("w2")).trim().split(",");
                List<String> values = new ArrayList(Arrays.asList(valueStr));
                values.forEach(v -> w2.add(Integer.parseInt(v.trim())));
            }

            Integer hourBin = null;
            if(paramMap.get("hour") != null)
                hourBin = Integer.parseInt((String)paramMap.get("hour"));

            Integer dayBin = null;
            if(paramMap.get("day") != null)
                dayBin = Integer.parseInt((String)paramMap.get("day"));

            boolean compare = (Boolean)paramMap.get("compare");
            boolean normalizeByTime = Boolean.parseBoolean((String)paramMap.get("normalizeByTime"));


            if(paramMap.keySet().contains("network")){
                Map<String, Double> southWest = (Map<String, Double>) ((Map)paramMap.get("bounds")).get("_southWest");
                Map<String, Double> northEast = (Map<String, Double>) ((Map)paramMap.get("bounds")).get("_northEast");
                Envelope env = new Envelope();
                env.expandToInclude(southWest.get("lng"), southWest.get("lat"));
                env.expandToInclude(northEast.get("lng"), northEast.get("lat"));
                List<SpatialDataItem> segments  = engine.getTrafficEngine().getStreetSegments(env);
                for(SpatialDataItem sdi : segments) {
                    StreetSegment streetSegment = (StreetSegment)sdi;
                    if(streetSegment != null) {
                        StatsVO statsVO = new StatsVO();
                        statsVOs.add(statsVO);
                        statsVO.edgeId = streetSegment.id;
                        statsVO.streetSegment = streetSegment;

                        if(compare) {
                            Integer confidenceInterval = Integer.parseInt((String)paramMap.get("confidenceInterval"));
                            SummaryStatistics stats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w1, new HashSet(hours));
                            SummaryStatistics stats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w2, new HashSet(hours));
                            SummaryStatisticsComparison statsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[confidenceInterval], stats1, stats2);
                            statsVO.summaryStatisticsComparison = statsComparison;
                            statsVO.summaryStatisticsCompare1 = stats1;
                            statsVO.summaryStatisticsCompare2 = stats2;
                        }else{
                            SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, true, w1, new HashSet(hours));
                            statsVO.summaryStatistics = summaryStatistics;
                        }
                    }
                }
            }else{

                //initRouting();

                //TODO: 'intermediate places' are in the api but the feature is broken: https://github.com/opentripplanner/OpenTripPlanner/issues/1784
                List<Fun.Tuple3<Long, Long, Long>> edges = new ArrayList<>();

                List routePoints = (List)paramMap.get("routePoints");
                for(int i = 0; i < routePoints.size() - 1; i++){

                    RoutingRequest rr = new RoutingRequest();
                    rr.useTraffic = hourBin == null ? false : true;  //if no hour specified, use the overall edge weights
                    if(hourBin != null){
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.HOUR_OF_DAY, hourBin);
                        System.out.println("querying for shortest route (local time): " + cal.getTime());
                        cal.add(Calendar.HOUR_OF_DAY, -utcAdjustment);
                        System.out.println("querying for shortest route (UTC time): " + cal.getTime());
                        cal.set(Calendar.DAY_OF_WEEK, dayBin);

                        rr.dateTime = cal.getTimeInMillis() / 1000;
                    }
                    rr.modes = new TraverseModeSet(TraverseMode.CAR);

                    Map<String, Double> routePoint = (Map)routePoints.get(i);
                    double lat = routePoint.get("lat");
                    double lng = routePoint.get("lng");
                    GenericLocation fromLocation = new GenericLocation(lat, lng);

                    routePoint = (Map)routePoints.get(i + 1);
                    lat = routePoint.get("lat");
                    lng = routePoint.get("lng");
                    GenericLocation toLocation = new GenericLocation(lat, lng);
                    rr.from = fromLocation;
                    rr.to = toLocation;
                    List<Fun.Tuple3<Long, Long, Long>> partialRoute = routing.route(rr);
                    edges.addAll(partialRoute);
                }

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

                                StatsVO statsVO = new StatsVO();
                                statsVOs.add(statsVO);
                                statsVO.edgeId = streetSegment.id;
                                statsVO.streetSegment = streetSegment;

                                if(compare) {
                                    Integer confidenceInterval = Integer.parseInt((String)paramMap.get("confidenceInterval"));
                                    SummaryStatistics stats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w1, new HashSet(hours));
                                    SummaryStatistics stats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w2, new HashSet(hours));
                                    SummaryStatisticsComparison statsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[confidenceInterval], stats1, stats2);
                                    statsVO.summaryStatisticsComparison = statsComparison;
                                    statsVO.summaryStatisticsCompare1 = stats1;
                                    statsVO.summaryStatisticsCompare2 = stats2;
                                }else{
                                    SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, true, w1, new HashSet(hours));
                                    statsVO.summaryStatistics = summaryStatistics;
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
            }

            String dir = "opentraffic_export_" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz").format(new Date());
            dir = dir.replace(":", "-");
            new File(dir).mkdir();
            List<StreetSegment> segments = new ArrayList<>();
            statsVOs.forEach(v -> segments.add(v.streetSegment));
            ShapefileUtil.create(segments, dir);

            DecimalFormat decimalFormatter = new DecimalFormat("#.00");

            StringBuilder builder = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            for(Integer week : w1) {



                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(SegmentStatistics.getTimeForWeek(week));


                ArrayList<String> dates = new ArrayList<String>();

                for(int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {

                    String date = sdf.format(cal.getTime());
                    dates.add(dayOfWeek, date);
                    cal.add(Calendar.DATE, 1); //bump day
                }

                PrintWriter pw = new PrintWriter(new File(dir + "/week_" + dates.get(0) + ".csv"));

                //date string,edge_id int,day_of_week int,hour_of_day int,avg_speed double,std_dev double,std_error double
                builder.append("date,edge_id,day_of_week,hour_of_day,avg_speed\n");

                for (StatsVO statsVO : statsVOs) {
                    if (statsVO.summaryStatistics != null) {
                        for (int hour = 0; hour < SegmentStatistics.HOURS_IN_WEEK; hour++) {

                            Double count = statsVO.summaryStatistics.hourCount.get(hour);
                            if (count < 1)
                                continue;

                            int dayOfWeek = (int)Math.floor((hour) / 24);

                            builder.append(dates.get(dayOfWeek) + ",");d

                            builder.append(statsVO.edgeId + ",");

                            builder.append(dayOfWeek + ",");
                            builder.append((hour) % 24 + ",");

                            //Double sum = statsVO.summaryStatistics.hourSum.get(hour);
                            Double mean = statsVO.summaryStatistics.getMean(hour);
                            //Double stdDev = statsVO.summaryStatistics.getStdDev(hour);

                            if (!Double.isNaN(mean)) {
                                mean = mean * 3.6;
//                                stdDev = stdDev * 3.6;
                            }
                            builder.append(decimalFormatter.format(mean));
//
//                            builder.append(stdDev + ",");
//                            builder.append(stdDev / Math.sqrt(count));


                            builder.append("\n");

                        }

                        pw.write(builder.toString());
                        builder = new StringBuilder();
                    }
                }

                pw.write(builder.toString());
                pw.flush();
                pw.close();


                log.info("writing csv for week " + week);
            }

            log.info("done.");

            //File directory = new File(dir);
            //ZipUtil.pack(directory, new File(dir + ".zip"));
            //FileUtils.cleanDirectory(directory);
            //new File(dir).delete();
            return dir + ".zip";
        });


        post("/route/save", (request, response) -> {

            String routeJson = request.body();
            Map<String, Object> paramMap = mapper.readValue(routeJson, new TypeReference<Map<String, Object>>() {});
            SavedRoute savedRoute = new SavedRoute();
            savedRoute.setName((String)paramMap.get("name"));
            savedRoute.setCountry((String)paramMap.get("country"));
            savedRoute.setCreationDate(new Date());
            savedRoute.setJson(routeJson);
//            if(user != null)
//                savedRoute.setUser(user);
//
//            AuthUtil.persistEntity(savedRoute);

            return savedRoute.getId();
        });

        get("/route/:id", (request, response) -> {
//            Integer id = new Integer(request.params(":id"));
//            SavedRoute savedRoute = AuthUtil.getSavedRoute(id);
//            return savedRoute.getJson();
            return response;
        });

        delete("/routelist/:id", (request, response) -> {
            Integer id = new Integer(request.params(":id"));
//            AuthUtil.deleteSavedRoute(id);
//            response.status(200);
             return response;
        });

        get("/routesbycountry/:country", (request, response) -> {
//            String country = request.params(":country");
//            Map<String, String> cookies = request.cookies();
//            String username = cookies.get("login_username");
//            String cookie = cookies.get("login_token");
//            if (cookie != null) {
//                User user = AuthUtil.login(username, null, cookie);
//                List<SavedRoute> routes = AuthUtil.getRoutesForUser(user, country);
//                response.status(200);
//                return mapper.writeValueAsString(routes);
//            }
            response.status(403);
            return response;
        });

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
		
		post("/route", (request, response) -> {

            //initRouting();

            Integer missingStatsSearchEnvelopeInMeters = Integer.parseInt(appProps.getProperty("application.missingStatsSearchEnvelopeInMeters"));

            Integer missingStatsMaxSamples = Integer.parseInt(appProps.getProperty("application.missingStatsMaxSamples"));

			response.header("Access-Control-Allow-Origin", "*");

            Map<String, Object> paramMap= mapper.readValue(request.body(), new TypeReference<Map<String, Object>>(){});

			List<Integer> utcCorrectedhours = new ArrayList<>();

            Integer utcAdjustment = (Integer)paramMap.get("utcAdjustment");

			if(paramMap.containsKey("h") && !((String)paramMap.get("h")).trim().isEmpty()) {
				String valueStr[] = ((String)paramMap.get("h")).trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
                for(String value : values){
                    int uncorrectedHour = new Integer(value);
                    int utcCorrectedHour = fixIncomingHour(uncorrectedHour, utcAdjustment);
                    utcCorrectedhours.add(utcCorrectedHour);
                }
			}


			Set<Integer> w1 = new HashSet<>();
			Set<Integer> w2 = new HashSet<>();

            if(paramMap.containsKey("w1") && !((String)paramMap.get("w1")).trim().isEmpty()) {
				String valueStr[] = ((String)paramMap.get("w1")).trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
				values.forEach(v -> w1.add(Integer.parseInt(v.trim())));
			}

            if(paramMap.containsKey("w2") && !((String)paramMap.get("w2")).trim().isEmpty()) {
				String valueStr[] = ((String)paramMap.get("w2")).trim().split(",");
				List<String> values = new ArrayList(Arrays.asList(valueStr));
				values.forEach(v -> w2.add(Integer.parseInt(v.trim())));
			}

            Integer hourBin = null;
            if(paramMap.get("hour") != null)
                hourBin = Integer.parseInt((String)paramMap.get("hour"));

            Integer dayBin = null;
            if(paramMap.get("day") != null)
                dayBin = Integer.parseInt((String)paramMap.get("day"));

            boolean compare = (Boolean)paramMap.get("compare");
            boolean normalizeByTime = Boolean.parseBoolean((String)paramMap.get("normalizeByTime"));
            String confidenceInterval = (String) paramMap.get("confidenceInterval");

            //TODO: 'intermediate places' are in the api but the feature is broken: https://github.com/opentripplanner/OpenTripPlanner/issues/1784
            List<Fun.Tuple3<Long, Long, Long>> edges = new ArrayList<>();

            List routePoints = (List)paramMap.get("routePoints");
            for(int i = 0; i < routePoints.size() - 1; i++){

                RoutingRequest rr = new RoutingRequest();
                rr.useTraffic = hourBin == null ? false : true;  //if no hour specified, use the overall edge weights
                if(hourBin != null){
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    cal.set(Calendar.DAY_OF_WEEK, dayBin);
                    cal.set(Calendar.HOUR_OF_DAY, hourBin);
                    System.out.println("querying for shortest route (local time): " + cal.getTime());
                    cal.add(Calendar.HOUR_OF_DAY, -utcAdjustment);
                    System.out.println("querying for shortest route (UTC time): " + cal.getTime());

                    rr.dateTime = cal.getTimeInMillis() / 1000;
                }
                rr.modes = new TraverseModeSet(TraverseMode.CAR);

                Map<String, Double> routePoint = (Map)routePoints.get(i);
                double lat = routePoint.get("lat");
                double lng = routePoint.get("lng");
                GenericLocation fromLocation = new GenericLocation(lat, lng);

                routePoint = (Map)routePoints.get(i + 1);
                lat = routePoint.get("lat");
                lng = routePoint.get("lng");
                GenericLocation toLocation = new GenericLocation(lat, lng);
                rr.from = fromLocation;
                rr.to = toLocation;
                List<Fun.Tuple3<Long, Long, Long>> partialRoute = routing.route(rr);
                edges.addAll(partialRoute);
            }

			TrafficPath trafficPath = new TrafficPath();

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
							SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, true, w1, new HashSet(utcCorrectedhours));
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
                                                    .collectSummaryStatistics(item.id, true, w1, new HashSet(utcCorrectedhours));
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

                                    summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(segmentIds, normalizeByTime, w1, new HashSet(utcCorrectedhours));
                                    summaryStatistics.inferred = true;
                                    trafficPath.inferred = true;
                                }
                                if(compare){
                                    SummaryStatistics stats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w1, new HashSet(utcCorrectedhours));
                                    SummaryStatistics stats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w2, new HashSet(utcCorrectedhours));
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
						} else {
							lastUnmatchedEdgeId = edgeId;
						}
					}
				} else {
					lastUnmatchedEdgeId = edgeId;
				}

			}

            SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(edgeIds, normalizeByTime, w1, null);
            trafficPath.averageSpeedForRouteInKph = Math.round((summaryStatistics.getMean() * 3.6) * 100.0) / 100.0;
            if(utcCorrectedhours.size() > 0){
                SummaryStatistics filteredSummaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(edgeIds, normalizeByTime, w1, new HashSet(utcCorrectedhours));
                trafficPath.averageSpeedForRouteInKph = Math.round((filteredSummaryStatistics.getMean() * 3.6) * 100.0) / 100.0;
            }

            if (w2.size() > 0) {
                SummaryStatistics summaryStats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(edgeIds,normalizeByTime, w2, null);
                SummaryStatisticsComparison summaryStatisticsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[Integer.parseInt(confidenceInterval)], summaryStatistics, summaryStats2);
                trafficPath.setWeeklyStats(new WeeklyStatsObject(summaryStatisticsComparison));
                for(int hour = 0; hour < (WeeklyStatsObject.HOURS_IN_WEEK); hour++) {
                    double mean1 = summaryStatistics.getMean(hour);
                    double mean2 = summaryStats2.getMean(hour);
                    double diff = summaryStatisticsComparison.difference(hour);
                    double diffPct = summaryStatisticsComparison.differenceAsPercent(hour);
                    System.out.println(hour+","+mean1+","+mean2+","+diff+","+diffPct);
                }
            }else{
                trafficPath.setWeeklyStats(summaryStatistics);
            }

            double distanceInMeters = 0;
            for(TrafficPathEdge segment : trafficPath.pathEdges){
                distanceInMeters += segment.length;
            }

            trafficPath.travelTimeInSeconds = ((distanceInMeters / 1000) / trafficPath.averageSpeedForRouteInKph) * 60 * 60;

            //put the speed data into hashmaps...
            Map<Integer, Double> hourCountMap = new TreeMap<>();
            Map<Integer, Double> hourSpeedMap = new TreeMap<>();
            Iterator<IntCursor> hourIter = summaryStatistics.hourCount.keys().iterator();
            while(hourIter.hasNext()){
                IntCursor hourCursor = hourIter.next();
                int hour = hourCursor.value;
                double count = summaryStatistics.hourCount.get(hour);
                if(!hourCountMap.keySet().contains(hour))
                    hourCountMap.put(hour, 0d);
                hourCountMap.put(hour, hourCountMap.get(hour) + count);
            }

            hourIter = summaryStatistics.hourSum.keys().iterator();
            while(hourIter.hasNext()){
                IntCursor hourCursor = hourIter.next();
                int hour = hourCursor.value;
                double speedSum = summaryStatistics.hourSum.get(hour);
                if(!hourSpeedMap.keySet().contains(hour))
                    hourSpeedMap.put(hour, 0d);
                hourSpeedMap.put(hour, hourSpeedMap.get(hour) + speedSum);
            }

            Map<Integer, Double> utcHoursToAvgSpeedsMap = new TreeMap<>();
            Map<Integer, Integer> utcHoursToCountsMap = new TreeMap<>();
            //divide speed sum by count, change from m/s to kph.
            for(Integer hour : hourCountMap.keySet()){
                utcHoursToAvgSpeedsMap.put(hour, (hourSpeedMap.get(hour) / hourCountMap.get(hour)) * 3.6);
                utcHoursToCountsMap.put(hour, hourCountMap.get(hour).intValue());
            }

            class SpeedInfo {
                public int count = 0;
                public int vehicleCount = 0;
                public double speed = 0d;
            }

            Map<Integer, SpeedInfo> hourOfDaySpeedMap = new TreeMap<>();
            Map<Integer, SpeedInfo> dayOfWeekSpeedMap = new TreeMap<>();
            Map<Integer, SpeedInfo> hourOfWeekSpeedMap = new TreeMap<>();
            Map<Integer, WeeklyStatsObject.HourStats> statsMap = new TreeMap<>();
            for(int i = 0; i < trafficPath.weeklyStats.hours.length; i++){
                WeeklyStatsObject.HourStats stats = trafficPath.weeklyStats.hours[i];
                stats.avg = Math.round(stats.s / stats.c * 100.0) / 100.0;
                statsMap.put(stats.h, stats);
            }

            for(Integer utcCorrectedHour: utcHoursToAvgSpeedsMap.keySet()){

                int localizedHour = fixOutgoingHour(utcCorrectedHour, utcAdjustment);

                Integer hourOfDay = localizedHour % 24;
                if(hourOfDay == 0)
                    hourOfDay = 24;
                Integer dayOfWeek = Math.round(((localizedHour - hourOfDay) / 24));

                if(!hourOfDaySpeedMap.keySet().contains(hourOfDay))
                    hourOfDaySpeedMap.put(hourOfDay, new SpeedInfo());
                if(!dayOfWeekSpeedMap.keySet().contains(dayOfWeek))
                    dayOfWeekSpeedMap.put(dayOfWeek, new SpeedInfo());
                if(!hourOfWeekSpeedMap.keySet().contains(localizedHour))
                    hourOfWeekSpeedMap.put(localizedHour, new SpeedInfo());

                SpeedInfo hourOfDayInfo = hourOfDaySpeedMap.get(hourOfDay);
                SpeedInfo dayOfWeekInfo = dayOfWeekSpeedMap.get(dayOfWeek);
                SpeedInfo hourOfWeekInfo = hourOfWeekSpeedMap.get(localizedHour);

                double speedSumForHour = utcHoursToAvgSpeedsMap.get(utcCorrectedHour);
                hourOfDayInfo.count = hourOfDayInfo.count + 1;
                hourOfDayInfo.vehicleCount = hourOfDayInfo.vehicleCount + utcHoursToCountsMap.get(utcCorrectedHour);
                hourOfDayInfo.speed = hourOfDayInfo.speed + speedSumForHour;
                dayOfWeekInfo.count = dayOfWeekInfo.count + 1;
                dayOfWeekInfo.vehicleCount = dayOfWeekInfo.vehicleCount + utcHoursToCountsMap.get(utcCorrectedHour);
                dayOfWeekInfo.speed = dayOfWeekInfo.speed + speedSumForHour;
                hourOfWeekInfo.count = hourOfWeekInfo.count + 1;
                hourOfWeekInfo.vehicleCount = hourOfWeekInfo.vehicleCount + utcHoursToCountsMap.get(utcCorrectedHour);
                hourOfWeekInfo.speed = hourOfWeekInfo.speed + speedSumForHour;

                if(statsMap.keySet().contains(utcCorrectedHour)){
                    WeeklyStatsObject.HourStats stats = statsMap.get(utcCorrectedHour);
                    stats.avg = Math.round(stats.s / stats.c * 100.0) / 100.0;
                    stats.hourOfDay = hourOfDay;
                    stats.dayOfWeek = dayOfWeek + 1;
                    stats.h = localizedHour;
                    stats.s = stats.s * 3.6;
                }
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

//        post("/login", (request, response) -> {
//            Map<String, String> params = getPostParams(request);
//            String username = params.get("username");
//            String password = params.get("password");
//            String cookie = params.get("token");
//            if(password != null)
//                password = PasswordUtil.hash(password);
//
//            User user = AuthUtil.login(username, password, cookie);
//
//            if(user != null){
//                return mapper.writeValueAsString(user);
//            }
//            response.status(403);
//            return "Authentication failed";
//        });
//
//        post("/users", (request, response) -> {
//            Map<String, String> params = getPostParams(request);
//            String username = java.net.URLDecoder.decode(params.get("username"), "UTF-8");
//            String password = params.get("password");
//            String role = java.net.URLDecoder.decode(params.get("role"), "UTF-8");
//            User u = new User();
//            u.setUsername(username);
//            u.setPasswordHash(PasswordUtil.hash(password));
//            u.setRole(role);
//            AuthUtil.persistEntity(u);
//            response.status(200);
//            return response;
//        });
//
//        put("/users/:id", (request, response) -> {
//            Map<String, String> params = getPostParams(request);
//            String username = java.net.URLDecoder.decode(params.get("username"), "UTF-8");
//            String password = params.get("password");
//            String role = java.net.URLDecoder.decode(params.get("role"), "UTF-8");
//            Integer id = new Integer(request.params(":id"));
//
//            User u = AuthUtil.getUser(id);
//            u.setUsername(username);
//            if(password != null && !password.isEmpty()) {
//              password = java.net.URLDecoder.decode(password, "UTF-8");
//              u.setPasswordHash(PasswordUtil.hash(password));
//            }
//            u.setRole(role);
//            AuthUtil.updateUser(u);
//            response.status(200);
//            return mapper.writeValueAsString(u);
//        });
//
//        delete("/users/:id", (request, response) -> {
//            Integer id = new Integer(request.params(":id"));
//            AuthUtil.deleteUser(id);
//            response.status(200);
//            return response;
//        });
//
//        get("/users", (request, response) -> {
//            return mapper.writeValueAsString(AuthUtil.getUsers());
//        });

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

    public static int fixIncomingHour(int uncorrectedHour, int utcAdjustment){
        int hour = Math.floorMod((uncorrectedHour - utcAdjustment - 1), 168);
        return hour;
    }

    public static int fixOutgoingHour(int uncorrectedHour, int utcAdjustment){
        int hour = Math.floorMod((uncorrectedHour + utcAdjustment + 1), 168);
        if(hour == 0)
            hour = 168;
        return hour;
    }



}
