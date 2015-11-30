package com.conveyal.traffic.app.routing;

import io.opentraffic.engine.data.SpatialDataItem;
import io.opentraffic.engine.data.stats.SummaryStatistics;
import io.opentraffic.engine.geom.StreetSegment;

import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.mapdb.Fun;
import org.opentripplanner.analyst.core.SlippyTile;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.traffic.Segment;
import org.opentripplanner.traffic.SegmentSpeedSample;
import org.opentripplanner.traffic.StreetSpeedSnapshot;
import org.opentripplanner.traffic.StreetSpeedSnapshotSource;

import com.beust.jcommander.internal.Maps;
import com.conveyal.traffic.app.TrafficEngineApp;
import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Routing with OpenTraffic data in a single area of the world.
 */
public class Routing {
    private static final Logger log = Logger.getLogger(Routing.class.getName());

    /** The graph to use for routing. */
    private Router graph;

    /** The graph path finder for the above graph */
    private GraphPathFinder gpf;

    /** The bounding box for this graph */
    private Rectangle boundingBox;

    /** Create a new router with the given bounding box */
    public Routing(Rectangle boundingBox) {
	this.boundingBox = boundingBox;
    }

    private boolean unbuilt = true;
    private boolean updated = false;

    /** Get a trip plan, or null if the graph is not yet built */
    public List<Fun.Tuple3<Long, Long, Long>> route(RoutingRequest request) {
	if (graph == null)
	    return null;

	Envelope env = new Envelope();
	env.expandToInclude(request.to.lng, request.to.lat);
	env.expandToInclude(request.from.lng, request.from.lat);

	// if(!updated)
	// update(env);

	List<GraphPath> paths = gpf.graphPathFinderEntryPoint(request);

	List<Fun.Tuple3<Long, Long, Long>> edges = new ArrayList<>();

	if (paths.size() > 0) {
	    for (Edge edge : paths.get(0).edges) {
		if (edge instanceof StreetEdge) {

		    StreetEdge streetEdge = (StreetEdge) edge;
		    if (streetEdge.wayId > 0) {
			Fun.Tuple3<Long, Long, Long> streetEdgeId = new Fun.Tuple3<>(
				streetEdge.wayId,
				streetEdge.getStartOsmNodeId(),
				streetEdge.getEndOsmNodeId());
			edges.add(streetEdgeId);
		    }

		}

	    }
	}

	return edges;
    }

    /** Update the traffic data in the graph */
    private void update(Envelope env) {
	Map<Segment, SegmentSpeedSample> samples = Maps.newHashMap();

	// not using an updater here as that requires dumping/loading PBFs.
	for (SpatialDataItem sdi : TrafficEngineApp.engine.getTrafficEngine()
		.getStreetSegments(env)) {
	    StreetSegment ss = (StreetSegment) sdi;
	    SummaryStatistics stats = TrafficEngineApp.engine
		    .getTrafficEngine().osmData.statsDataStore
		    .collectSummaryStatistics(ss.id, null, null);

	    if (stats == null || Double.isNaN(stats.getMean()))
		continue;

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

	if (!samples.isEmpty()) {
	    log.log(Level.INFO, "Applying " + samples.size()
		    + " speed samples to graph");

	    if (graph.graph.streetSpeedSource == null)
		graph.graph.streetSpeedSource = new StreetSpeedSnapshotSource();

	    graph.graph.streetSpeedSource.setSnapshot(new StreetSpeedSnapshot(
		    samples));
	} else {
	    log.log(Level.INFO, "Found no samples to apply to graph.");

	    // clear existing samples
	    graph.graph.streetSpeedSource = null;
	}

	updated = true;
    }

    public void buildIfUnbuilt() {
	if (unbuilt)
	    build();
    }

    /** (Re)-build the graph (async method) */
    public void build() {
	unbuilt = false;
	ExecutorService es = Executors.newCachedThreadPool();
	es.execute(new BuildGraphs(this));
    }

    /** Build graphs so we can do routing with traffic data */
    public static class BuildGraphs implements Runnable {
	private final Routing routing;

	public BuildGraphs(Routing routing) {
	    this.routing = routing;
	}

	@Override
	public void run() {
	    // gather input files
	    List<File> infiles = Lists.newArrayList();

	    // find all the OSM files we need
	    File cacheDir = new File(
		    TrafficEngineApp.appProps
			    .getProperty("application.data.osmDirectory"));

	    Z: for (File zfile : cacheDir.listFiles()) {
		int z = Integer.parseInt(zfile.getName());
		X: for (File xfile : zfile.listFiles()) {
		    int x = Integer.parseInt(xfile.getName());

		    log.log(Level.INFO, "x: " + x);

		    double west = SlippyTile.tile2lon(x, z);
		    double east = SlippyTile.tile2lon(x + 1, z);

		    // check if this could possibly contain the right file
		    if (east < routing.boundingBox.getMinX()
			    || west > routing.boundingBox.getMaxX())
			continue X;

		    Y: for (File yfile : xfile.listFiles()) {
			int y = Integer.parseInt(yfile.getName().replace(
				".osm.pbf", ""));

			double north = SlippyTile.tile2lat(y, z);
			double south = SlippyTile.tile2lat(y + 1, z);

			log.log(Level.INFO, "y: " + y + ", n: " + north
				+ ", s: " + south);

			log.log(Level.INFO,
				"min: " + routing.boundingBox.getMinY()
					+ ", max: "
					+ routing.boundingBox.getMaxY());

			if (north < routing.boundingBox.getMinY()
				|| south > routing.boundingBox.getMaxY())
			    continue Y;

			infiles.add(yfile);
		    }
		}
	    }

	    log.log(Level.INFO, "Using " + infiles.size() + " tiles for graph");

	    // phew. having figured out what tiles we need now build an OTP
	    // graph.
	    // note we do not configure an updater: we don't need one, as we do
	    // the updates
	    // ourselves.
	    GraphBuilder gb = new GraphBuilder();
	    OpenStreetMapModule osm = new OpenStreetMapModule();
	    osm.setProviders(infiles
		    .stream()
		    .map(osmFile -> new AnyFileBasedOpenStreetMapProviderImpl(
			    osmFile)).collect(Collectors.toList()));
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

	    log.log(Level.INFO, "graph built");
	}
    }
}
