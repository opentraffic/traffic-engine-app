package io.opentraffic.engine.app;

import com.carrotsearch.hppc.IntCollection;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.ShortCursor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.IterableSerializer;
import com.google.common.net.MediaType;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import io.opentraffic.engine.app.data.*;
import io.opentraffic.engine.app.engine.Engine;
import io.opentraffic.engine.app.routing.Routing;
import io.opentraffic.engine.app.tiles.TrafficTileRequest;
import io.opentraffic.engine.app.util.HibernateUtil;
import io.opentraffic.engine.app.util.PasswordUtil;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        HibernateUtil.getSessionFactory();

		engine = new Engine();

        String portString = appProps.getProperty("application.port");
        if(!StringUtils.isEmpty(portString)){
            port(Integer.parseInt(portString));
        }



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

            Map<String, String> cookies = request.cookies();
            String username = cookies.get("login_username");
            String cookie = cookies.get("login_token");
            boolean isAdmin = false;
            if(cookie != null){
                User user = HibernateUtil.login(username, null, cookie);
                if(user.getRole().equalsIgnoreCase("super admin") || user.getRole().equalsIgnoreCase("super admin"))
                    isAdmin = true;
            }

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

            Set<Integer> hours = new HashSet<>();

            if(paramMap.containsKey("h") && !((String)paramMap.get("h")).trim().isEmpty()) {
                String valueStr[] = ((String)paramMap.get("h")).trim().split(",");
                List<String> values = new ArrayList(Arrays.asList(valueStr));
                values.forEach(v -> hours.add(Integer.parseInt(v.trim())));
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
                            SummaryStatistics stats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w1, hours);
                            SummaryStatistics stats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w2, hours);
                            SummaryStatisticsComparison statsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[confidenceInterval], stats1, stats2);
                            statsVO.summaryStatisticsComparison = statsComparison;
                            statsVO.summaryStatisticsCompare1 = stats1;
                            statsVO.summaryStatisticsCompare2 = stats2;
                        }else{
                            SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, true, w1, hours);
                            statsVO.summaryStatistics = summaryStatistics;
                        }
                    }
                }
            }else{
                while(!routing.isReady()){
                    log.info("Graph not ready, waiting 1 second");
                    Thread.sleep(1000);
                }
                //TODO: 'intermediate places' are in the api but the feature is broken: https://github.com/opentripplanner/OpenTripPlanner/issues/1784
                List<Fun.Tuple3<Long, Long, Long>> edges = new ArrayList<>();

                List routePoints = (List)paramMap.get("routePoints");
                for(int i = 0; i < routePoints.size() - 1; i++){

                    RoutingRequest rr = new RoutingRequest();
                    rr.useTraffic = hourBin == null ? false : true;  //if no hour specified, use the overall edge weights
                    if(hourBin != null){
                        DateTime time = new DateTime(DateTimeZone.UTC).dayOfMonth().withMinimumValue();
                        time = time.withDayOfWeek(dayBin);
                        time = time.withHourOfDay(hourBin);

                        rr.dateTime = time.getMillis() / 1000;
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
                                    SummaryStatistics stats1 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w1, hours);
                                    SummaryStatistics stats2 = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, normalizeByTime, w2, hours);
                                    SummaryStatisticsComparison statsComparison = new SummaryStatisticsComparison(SummaryStatisticsComparison.PValue.values()[confidenceInterval], stats1, stats2);
                                    statsVO.summaryStatisticsComparison = statsComparison;
                                    statsVO.summaryStatisticsCompare1 = stats1;
                                    statsVO.summaryStatisticsCompare2 = stats2;
                                }else{
                                    SummaryStatistics summaryStatistics = TrafficEngineApp.engine.getTrafficEngine().osmData.statsDataStore.collectSummaryStatistics(streetSegment.id, true, w1, hours);
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

            StringBuilder builder = new StringBuilder();
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

            Integer minHour = 0;
            Integer maxHour = 23;
            if(hours != null && hours.size() > 0){
                minHour = Collections.min(hours);
                maxHour = Collections.max(hours);
            }

            String dayBooleanString = "1,1,1,1,1,1,1,";
            if(hours != null && hours.size() > 0){
                dayBooleanString = "";
                Integer startDay = (minHour  -  1) / 24;
                Integer endDay = ((maxHour - minHour) / 24) + startDay;
                for(int i = 0; i < 7; i++){
                    if(i >= startDay && i <= endDay){
                        dayBooleanString += "1,";
                    }else{
                        dayBooleanString += "0,";
                    }
                }
            }


            if(compare){
                Integer confidenceInterval = Integer.parseInt((String)paramMap.get("confidenceInterval"));
                if(isAdmin){
                    builder.append("Edge Id,Date Start (Baseline),Date End (Baseline),Date Start (Comparison),Date End (Comparison),Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday,Time Start,Time End,Percent Change,Confidence Interval,Alpha,T-Score,Degrees of Freedom,Margin of Error,Normalized by Time,Average Speed (Baseline),Number of Observations (Baseline),Standard Deviation (Baseline),Standard Error (Baseline),99% Upper Bound (Baseline),99% Lower Bound (Baseline),97% Upper Bound (Baseline),97% Lower Bound (Baseline),95% Upper Bound (Baseline),95% Lower Bound (Baseline),90% Upper Bound (Baseline),90% Lower Bound (Baseline),Average Speed (Comparison),Number of Observations (Comparison),Standard Deviation (Comparison),Standard Error (Comparison),99% Upper Bound (Comparison),99% Lower Bound (Comparison),97% Upper Bound (Comparison),97% Lower Bound (Comparison),95% Upper Bound (Comparison),95% Lower Bound (Comparison),90% Upper Bound (Comparison),90% Lower Bound (Comparison)\n");
                }else{
                    builder.append("Edge Id,Date Start (Baseline),Date End (Baseline),Date Start (Comparison),Date End (Comparison),Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday,Time Start,Time End,Percent Change,Confidence Interval,Alpha,T-Score,Degrees of Freedom,Margin of Error,Normalized by Time,Average Speed (Baseline),Standard Deviation (Baseline),Standard Error (Baseline),99% Upper Bound (Baseline),99% Lower Bound (Baseline),97% Upper Bound (Baseline),97% Lower Bound (Baseline),95% Upper Bound (Baseline),95% Lower Bound (Baseline),90% Upper Bound (Baseline),90% Lower Bound (Baseline),Average Speed (Comparison),Standard Deviation (Comparison),Standard Error (Comparison),99% Upper Bound (Comparison),99% Lower Bound (Comparison),97% Upper Bound (Comparison),97% Lower Bound (Comparison),95% Upper Bound (Comparison),95% Lower Bound (Comparison),90% Upper Bound (Comparison),90% Lower Bound (Comparison)\n");
                }
                for(StatsVO statsVO : statsVOs){
                    if(statsVO.summaryStatisticsComparison != null){
                        for(int i = 0; i < SegmentStatistics.HOURS_IN_WEEK; i++){
                            Double count = statsVO.summaryStatisticsCompare1.hourCount.get(i);
                            if(count < 1)
                                continue;

                            builder.append(statsVO.edgeId + ",");
                            Date start = new Date(SegmentStatistics.getTimeForWeek(Collections.min(w1)));
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(SegmentStatistics.getTimeForWeek(Collections.max(w1)));
                            cal.add(Calendar.DATE, 6); //bump end date to end of week
                            builder.append(sdf.format(start) + ",");//Date Start (Baseline)
                            builder.append(sdf.format(cal.getTime()) + ",");//Date End (Baseline)

                            start = new Date(SegmentStatistics.getTimeForWeek(Collections.min(w2)));
                            cal = Calendar.getInstance();
                            cal.setTimeInMillis(SegmentStatistics.getTimeForWeek(Collections.max(w2)));
                            cal.add(Calendar.DATE, 6); //bump end date to end of week
                            builder.append(sdf.format(start) + ",");//Date Start (Comparison)
                            builder.append(sdf.format(cal.getTime()) + ",");//Date End (Comparison)

                            builder.append(dayBooleanString);//Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday
                            int hourIndex = i;
                            if(i > 24)
                                hourIndex = hourIndex % 24;
                            if(i < 10)
                                builder.append("0");
                            builder.append(hourIndex + ":00,");

                            if(i < 10)
                                builder.append("0");
                            builder.append(hourIndex + ":59,");
                            builder.append(statsVO.summaryStatisticsComparison.differenceAsPercent(i) + ",");
                            builder.append(confidenceInterval + ",");
                            builder.append(1 - confidenceInterval + ",");
                            builder.append(statsVO.summaryStatisticsComparison.tStat(i) + ",");
                            builder.append((statsVO.summaryStatisticsCompare1.count + statsVO.summaryStatisticsCompare2.count - 2) + ",");//Degrees of Freedom, df=n1+n2−2

                            double tCrit = statsVO.summaryStatisticsComparison.tCrit(i);
                            double stdDev = statsVO.summaryStatisticsComparison.combinedStdDev(i);
                            double combinedN = statsVO.summaryStatisticsComparison.getMeanSize(i);
                            double marginOfError = tCrit * (stdDev / Math.sqrt(combinedN));
                            builder.append(marginOfError + ","); // Margin of Error: tCrit * (std dev / √n)
                            builder.append(normalizeByTime + ","); //normalize by time
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) + ","); //Average Speed (Baseline),
                            if(isAdmin)
                                builder.append(statsVO.summaryStatisticsCompare1.hourCount.get(i) + ",");// Number of Observations (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getStdDev(i) + ","); //,Standard Deviation (Baseline),
                            stdDev = statsVO.summaryStatisticsCompare1.getStdDev(i);
                            double stdError = Math.sqrt(((stdDev * stdDev) / statsVO.summaryStatisticsCompare1.hourCount.get(i)) + ((stdDev * stdDev) / statsVO.summaryStatisticsCompare2.hourCount.get(i) ));
                            builder.append(stdError + ",");// Standard Error (Baseline),  square.root[(sd2/na) + (sd2/nb)]

                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) + (2.58 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 99% Upper Bound (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) - (2.58 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 99% Lower Bound (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) + (2.17 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 97% Upper Bound (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) - (2.17 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 97% Lower Bound (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) + (1.96 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 95% Upper Bound (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) - (1.96 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 95% Lower Bound (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) + (1.64 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 90% Upper Bound (Baseline)
                            builder.append(statsVO.summaryStatisticsCompare1.getMean(i) - (1.64 * statsVO.summaryStatisticsCompare1.getStdDev(i)) + ","); // 90% Lower Bound (Baseline)

                            if(isAdmin)
                                builder.append(statsVO.summaryStatisticsCompare2.getStdDev(i) + ","); // Number of Observations (Comparison),
                            builder.append(statsVO.summaryStatisticsCompare2.getStdDev(i) + ",");// Standard Deviation (Comparison),
                            stdDev = statsVO.summaryStatisticsCompare2.getStdDev(i);
                            stdError = Math.sqrt(((stdDev * stdDev) / statsVO.summaryStatisticsCompare2.hourCount.get(i)) + ((stdDev * stdDev) / statsVO.summaryStatisticsCompare1.hourCount.get(i) ));
                            builder.append(stdError + ",");// Standard Error (Comparison),

                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) + (2.58 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 99% Upper Bound (Comparison)
                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) - (2.58 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 99% Lower Bound (Comparison)
                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) + (2.17 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 97% Upper Bound (Comparison)
                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) - (2.17 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 97% Lower Bound (Comparison)
                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) + (1.96 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 95% Upper Bound (Comparison)
                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) - (1.96 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 95% Lower Bound (Comparison)
                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) + (1.64 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 90% Upper Bound (Comparison)
                            builder.append(statsVO.summaryStatisticsCompare2.getMean(i) - (1.64 * statsVO.summaryStatisticsCompare2.getStdDev(i)) + ","); // 90% Lower Bound (Comparison)
                            builder.append("\n");
                        }
                    }
                }
            }else{
                if(isAdmin){
                    builder.append("Edge Id,Date Start,Date End,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday,Time Start,Time End,Average Speed,Number of Observations,Standard Deviation,Standard Error,99% Upper Bound,99% Lower Bound,97% Upper Bound,97% Lower Bound,95% Upper Bound,95% Lower Bound,90% Upper Bound,90% Lower Bound\n");
                }else{
                    builder.append("Edge Id,Date Start,Date End,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday,Sunday,Time Start,Time End,Average Speed,Standard Deviation,Standard Error,99% Upper Bound,99% Lower Bound,97% Upper Bound,97% Lower Bound,95% Upper Bound,95% Lower Bound,90% Upper Bound,90% Lower Bound\n");
                }
                for(StatsVO statsVO : statsVOs){
                    if(statsVO.summaryStatistics != null){
                        for(int i = 0; i < SegmentStatistics.HOURS_IN_WEEK; i++){

                            Double count = statsVO.summaryStatistics.hourCount.get(i);
                            if(count < 1)
                                continue;

                            builder.append(statsVO.edgeId + ",");
                            Date start = new Date(SegmentStatistics.getTimeForWeek(Collections.min(w1)));
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(SegmentStatistics.getTimeForWeek(Collections.max(w1)));
                            cal.add(Calendar.DATE, 6); //bump end date to end of week
                            builder.append(sdf.format(start) + ",");
                            builder.append(sdf.format(cal.getTime()) + ",");
                            builder.append(dayBooleanString);
                            int hourIndex = i;
                            if(i > 24)
                                hourIndex = hourIndex % 24;
                            if(i < 10)
                                builder.append("0");
                            builder.append(hourIndex + ":00,");

                            if(i < 10)
                                builder.append("0");
                            builder.append(hourIndex + ":59,");

                            Double sum = statsVO.summaryStatistics.hourSum.get(i);
                            Double mean = statsVO.summaryStatistics.getMean(i);
                            Double stdDev = statsVO.summaryStatistics.getStdDev(i);
                            builder.append(mean + ",");
                            if(isAdmin)
                                builder.append(count + ",");
                            builder.append(stdDev + ",");
                            builder.append(stdDev / Math.sqrt(count));
                            builder.append(",");
                            builder.append(mean + (2.58 * stdDev)); //99% Upper Bound
                            builder.append(",");
                            builder.append(mean - (2.58 * stdDev)); //99% Lower Bound
                            builder.append(",");
                            builder.append(mean + (2.17 * stdDev)); //97% Upper Bound
                            builder.append(",");
                            builder.append(mean - (2.17 * stdDev)); //97% Lower Bound
                            builder.append(",");
                            builder.append(mean + (1.96 * stdDev)); //95% Upper Bound
                            builder.append(",");
                            builder.append(mean - (1.96 * stdDev)); //95% Lower Bound
                            builder.append(",");
                            builder.append(mean + (1.64 * stdDev)); //90% Upper Bound
                            builder.append(",");
                            builder.append(mean - (1.64 * stdDev)); //90% Lower Bound
                            builder.append("\n");
                        }
                    }
                }
            }

            PrintWriter pw = new PrintWriter(new File( dir + "/" + dir + ".csv"));
            pw.write(builder.toString());
            pw.flush();
            pw.close();
            File directory = new File(dir);
            ZipUtil.pack(directory, new File(dir + ".zip"));
            FileUtils.cleanDirectory(directory);
            new File(dir).delete();
            return dir + ".zip";
        });


        post("/route/save", (request, response) -> {
            Map<String, String> cookies = request.cookies();
            String username = cookies.get("login_username");
            String cookie = cookies.get("login_token");
            User user = null;
            if(cookie != null){
                user = HibernateUtil.login(username, null, cookie);
            }

            String routeJson = request.body();
            Map<String, Object> paramMap = mapper.readValue(routeJson, new TypeReference<Map<String, Object>>() {});
            SavedRoute savedRoute = new SavedRoute();
            savedRoute.setName((String)paramMap.get("name"));
            savedRoute.setCountry((String)paramMap.get("country"));
            savedRoute.setCreationDate(new Date());
            savedRoute.setJson(routeJson);
            if(user != null)
                savedRoute.setUser(user);

            HibernateUtil.persistEntity(savedRoute);

            return savedRoute.getId();
        });

        get("/route/:id", (request, response) -> {
            Integer id = new Integer(request.params(":id"));
            SavedRoute savedRoute = HibernateUtil.getSavedRoute(id);
            return savedRoute.getJson();
        });

        delete("/routelist/:id", (request, response) -> {
            Integer id = new Integer(request.params(":id"));
            HibernateUtil.deleteSavedRoute(id);
            response.status(200);
            return response;
        });

        get("/routesbycountry/:country", (request, response) -> {
            String country = request.params(":country");
            Map<String, String> cookies = request.cookies();
            String username = cookies.get("login_username");
            String cookie = cookies.get("login_token");
            if (cookie != null) {
                User user = HibernateUtil.login(username, null, cookie);
                List<SavedRoute> routes = HibernateUtil.getRoutesForUser(user, country);
                response.status(200);
                return mapper.writeValueAsString(routes);
            }
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

            while(!routing.isReady()){
                log.info("Graph not ready, waiting 1 second");
                Thread.sleep(1000);
            }

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
                    int localizedHour = fixOutgoingHour(utcCorrectedHour, utcAdjustment);
                    if(localizedHour != uncorrectedHour)
                        throw new RuntimeException();

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
                    DateTime time = new DateTime(DateTimeZone.UTC).dayOfMonth().withMinimumValue();
                    time = time.withDayOfWeek(dayBin);
                    time = time.withHourOfDay(hourBin);

                    rr.dateTime = time.getMillis() / 1000;
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

            int measurementCount = 0;
            double speedSum = 0;
            for(TrafficPathEdge segment : trafficPath.pathEdges){
                if(utcCorrectedhours.size() > 0){
                    for(Integer hour : utcCorrectedhours){
                        if(segment.countMap.containsKey(hour)){
                            double avgSpeedForHour = segment.speedMap.get(hour) / segment.countMap.get(hour);
                            measurementCount++;
                            speedSum += avgSpeedForHour;
                        }
                    }
                }else{
                    speedSum += segment.speed;
                    measurementCount++;
                }
            }

            Double avgSpeedForRoute = (speedSum / measurementCount) * 3.6;

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
                speedSum = summaryStatistics.hourSum.get(hour);
                if(!hourSpeedMap.keySet().contains(hour))
                    hourSpeedMap.put(hour, 0d);
                hourSpeedMap.put(hour, hourSpeedMap.get(hour) + speedSum);
            }

            Map<Integer, Double> utcHoursToAvgSpeedsMap = new TreeMap<>();
            //divide speed sum by count, change from m/s to kph.
            for(Integer hour : hourCountMap.keySet()){
                utcHoursToAvgSpeedsMap.put(hour, (hourSpeedMap.get(hour) / hourCountMap.get(hour)) * 3.6);
            }

            class SpeedInfo {
                public int count = 0;
                public double speed = 0d;
            }

            Map<Integer, SpeedInfo> hourOfDaySpeedMap = new TreeMap<>();
            Map<Integer, SpeedInfo> dayOfWeekSpeedMap = new TreeMap<>();
            Map<Integer, Double> hourOfWeekSpeedMap = new TreeMap<>();
            Map<Integer, WeeklyStatsObject.HourStats> statsMap = new TreeMap<>();
            for(int i = 0; i < trafficPath.weeklyStats.hours.length; i++){
                WeeklyStatsObject.HourStats stats = trafficPath.weeklyStats.hours[i];
                stats.avg = Math.round(stats.s / stats.c * 100.0) / 100.0;
                statsMap.put(stats.h, stats);
            }

            for(Integer utcCorrectedHour: utcHoursToAvgSpeedsMap.keySet()){

                int localizedHour = fixOutgoingHour(utcCorrectedHour, utcAdjustment);

                Integer hourOfDay = localizedHour % 24;
                Integer dayOfWeek = Math.round(((localizedHour - hourOfDay) / 24));

                if(!hourOfDaySpeedMap.keySet().contains(hourOfDay))
                    hourOfDaySpeedMap.put(hourOfDay, new SpeedInfo());
                if(!dayOfWeekSpeedMap.keySet().contains(dayOfWeek))
                    dayOfWeekSpeedMap.put(dayOfWeek, new SpeedInfo());
                if(!hourOfWeekSpeedMap.keySet().contains(localizedHour))
                    hourOfWeekSpeedMap.put(localizedHour, 0d);

                SpeedInfo hourOfDayInfo = hourOfDaySpeedMap.get(hourOfDay);
                SpeedInfo dayOfWeekInfo = dayOfWeekSpeedMap.get(dayOfWeek);

                double speedSumForHour = utcHoursToAvgSpeedsMap.get(utcCorrectedHour);
                hourOfDayInfo.count = hourOfDayInfo.count + 1;
                hourOfDayInfo.speed = hourOfDayInfo.speed + speedSumForHour;
                dayOfWeekInfo.count = dayOfWeekInfo.count + 1;
                dayOfWeekInfo.speed = dayOfWeekInfo.speed + speedSumForHour;
                hourOfWeekSpeedMap.put(localizedHour, speedSumForHour);

                if(statsMap.keySet().contains(utcCorrectedHour)){
                    WeeklyStatsObject.HourStats stats = statsMap.get(utcCorrectedHour);
                    stats.avg = Math.round(stats.s / stats.c * 100.0) / 100.0;
                    stats.hourOfDay = hourOfDay;
                    stats.dayOfWeek = dayOfWeek;
                    stats.h = localizedHour;
                    stats.s = stats.s * 3.6;
                }
            }

            System.out.println("hour of day,avg");
            for(int i = 0; i < 24; i++){
                if(hourOfDaySpeedMap.keySet().contains(i)){
                    SpeedInfo info = hourOfDaySpeedMap.get(i);
                    System.out.println(i + "," + info.speed / info.count);
                }else{
                    System.out.println(i + ",0");
                }
            }

            System.out.println("day of week,avg");
            for(int i = 0; i < 7; i++){
                if(dayOfWeekSpeedMap.keySet().contains(i)){
                    SpeedInfo info = dayOfWeekSpeedMap.get(i);
                    System.out.println(i + "," + info.speed / info.count);
                }else{
                    System.out.println(i + ",0");
                }
            }

            System.out.println("hour of week,avg");
            for(int i = 0; i < 168; i++){
                if(hourOfWeekSpeedMap.keySet().contains(i)){
                    Double speed = hourOfWeekSpeedMap.get(i);
                    System.out.println(i + "," + speed);
                }else{
                    System.out.println(i + ",0");
                }
            }

            trafficPath.averageSpeedForRouteInKph = Math.round(avgSpeedForRoute * 100.0) / 100.0;
            System.out.println("average speed for route: " + avgSpeedForRoute);

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
            HibernateUtil.persistEntity(u);
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

    public static int fixIncomingHour(int uncorrectedHour, int utcAdjustment){
        int hour = uncorrectedHour - utcAdjustment - 1;
        if(hour < 0)
            hour += 167;
        System.out.println("adjusted incoming hour from " + uncorrectedHour + " to " + hour);
        return hour;
    }

    public static int fixOutgoingHour(int uncorrectedHour, int utcAdjustment){
        int hour = uncorrectedHour + utcAdjustment + 1;
        if(hour > 167)
            hour -= 167;
        System.out.println("adjusted outgoing hour from " + uncorrectedHour + " to " + hour);
        return hour;
    }

}
