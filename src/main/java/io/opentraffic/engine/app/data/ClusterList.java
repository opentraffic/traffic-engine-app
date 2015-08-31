package io.opentraffic.engine.app.data;

import io.opentraffic.engine.osm.OSMCluster;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kpw on 8/20/15.
 */
public class ClusterList {

    public ArrayList<Cluster> clusters = new ArrayList<>();
    public ClusterList(List<OSMCluster> osmClusters) {

        for(OSMCluster osmCluster : osmClusters) {
            if(osmCluster.name != null) {
                Cluster cluster = new Cluster();
                cluster.name = osmCluster.name;
                cluster.lat = osmCluster.bounds.centre().y;
                cluster.lon = osmCluster.bounds.centre().x;
                clusters.add(cluster);
            }
        }

    }
}
