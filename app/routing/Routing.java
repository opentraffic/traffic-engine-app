package routing;

import com.beust.jcommander.internal.Maps;
import com.conveyal.traffic.data.SpatialDataItem;
import com.conveyal.traffic.geom.StreetSegment;
import com.conveyal.traffic.stats.BaselineStatistics;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;
import controllers.Application;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.GraphPathToTripPlanConverter;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
//import org.opentripplanner.traffic.Segment;
//import org.opentripplanner.traffic.SegmentSpeedSample;
//import org.opentripplanner.traffic.StreetSpeedSource;
import play.Logger;
import play.Play;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
//import java.util.stream.Collectors;

/**
 * Routing with OpenTraffic data in a single area of the world.
 */
public class Routing {
    /** The graph to use for routing. */
    private Router graph;

    /** The graph path finder for the above graph */
    private GraphPathFinder gpf;

    /** The bounding box for this graph */
    private Rectangle boundingBox;

    /** Create a new router with the given bounding box */
    public Routing (Rectangle boundingBox) {
        this.boundingBox = boundingBox;
    }

    private boolean unbuilt = true;

    /** Get a trip plan, or null if the graph is not yet built */
    public TripPlan route (RoutingRequest request) {
        if (graph == null)
            return null;

        update();

        List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);
        return GraphPathToTripPlanConverter.generatePlan(paths, request);
    }

    /** Update the traffic data in the graph */
    private void update() {
       /* Map<Segment, SegmentSpeedSample> samples = Maps.newHashMap();

        // not using an updater here as that requires dumping/loading PBFs.
        for (Envelope env : Application.engine.getOsmEnvelopes()) {
            if (env.getMaxX() > boundingBox.getMinX() && env.getMinX() < boundingBox.getMaxX() &&
                    env.getMaxY() > boundingBox.getMinY() && env.getMinY() < boundingBox.getMaxX()) {
                // we want to use speed samples from this envelope
                for (SpatialDataItem sdi : Application.engine.getStreetSegments(env)) {
                    StreetSegment ss = (StreetSegment) sdi;
                    BaselineStatistics stats =
                            Application.engine.getTrafficEngine().getSegementStatistics(ss.id);

                    SegmentSpeedSample samp;
                    try {
                        samp = new SegmentSpeedSample(stats);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    Segment seg = new Segment(ss.wayId, ss.startNodeId, ss.endNodeId);
                    samples.put(seg, samp);
                }
            }
        }

        if (!samples.isEmpty()) {
            Logger.info("Applying {} speed samples to graph", samples.size());

            if (graph.graph.streetSpeedSource == null)
                graph.graph.streetSpeedSource = new StreetSpeedSource();

            graph.graph.streetSpeedSource.setSamples(samples);
        }
        else {
            Logger.info("Found no samples to apply to graph.");

            // clear existing samples
            graph.graph.streetSpeedSource = null;
        } */
    }

    public void buildIfUnbuilt () {
        if (unbuilt) build();
    }

    /** (Re)-build the graph (async method) */
    public void build () {
        unbuilt = false;
         Akka.system().scheduler().scheduleOnce(
                Duration.create(10, "milliseconds"),
                new BuildGraphs(this),
                Akka.system().dispatcher()
        );
    }

    /** Build graphs so we can do routing with traffic data */
    public static class BuildGraphs implements Runnable {
        private final Routing routing;

        public BuildGraphs (Routing routing) {
            this.routing = routing;
        }

        @Override
        public void run() {
            // gather input files
            List<File> infiles = Lists.newArrayList();

            // find all the OSM files we need
            File cacheDir = new File(Play.application().configuration().getString("application.data.osmDirectory"));

            Z: for (File zfile : cacheDir.listFiles()) {
                int z = Integer.parseInt(zfile.getName());
                X: for (File xfile : zfile.listFiles()) {
                    int x = Integer.parseInt(xfile.getName());

                    Logger.info("x: {}", x);

                    double west = SlippyTile.tile2lon(x, z);
                    double east = SlippyTile.tile2lon(x + 1, z);

                    // check if this could possibly contain the right file
                    if (east < routing.boundingBox.getMinX() || west > routing.boundingBox.getMaxX())
                        continue X;

                    Y: for (File yfile : xfile.listFiles()) {
                        int y = Integer.parseInt(yfile.getName().replace(".osm.pbf", ""));

                        double north = SlippyTile.tile2lat(y, z);
                        double south = SlippyTile.tile2lat(y + 1, z);

                        Logger.info("y: {}, n: {}, s: {}", y, north, south);

                        Logger.info("min: {}, max: {}", routing.boundingBox.getMinY(), routing.boundingBox.getMaxY());

                        if (north < routing.boundingBox.getMinY() || south > routing.boundingBox.getMaxY())
                            continue Y;

                        infiles.add(yfile);
                    }
                }
            }

            Logger.info("Using {} tiles for graph", infiles.size());

            // phew. having figured out what tiles we need now build an OTP graph.
            // note we do not configure an updater: we don't need one, as we do the updates
            // ourselves.
           /* GraphBuilder gb = new GraphBuilder();
            OpenStreetMapModule osm = new OpenStreetMapModule();
            osm.setProviders(infiles.stream()
                            .map(osmFile -> new AnyFileBasedOpenStreetMapProviderImpl(osmFile))
                            .collect(Collectors.toList()));
            gb.addModule(osm);
            gb.serializeGraph = false;
            gb.run();
            Graph g = gb.getGraph();
            Router r = new Router("default", g);
            GraphPathFinder gpf = new GraphPathFinder(r);

            g.index(new DefaultStreetVertexIndexFactory());

            synchronized (routing) {
                routing.graph = r;
                routing.gpf = gpf;
            }

            Logger.info("graph built");*/
        } 
    }
}
