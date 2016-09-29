package io.opentraffic.engine.app.util;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import io.opentraffic.engine.geom.StreetSegment;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by dbenoff on 2/20/16.
 */
public class ShapefileUtil {
    private static final Logger log = Logger.getLogger(ShapefileUtil.class.getName());

    public static void create(List<StreetSegment> segments, String filePath) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

        //set the name
        b.setName( "Opentraffic" );
        //add a geometry property

        //b.add( "route", LineString.class );
        b.add( "the_geom", LineString.class );
        b.add( "segment_id", Long.class);

        //build the type
        final SimpleFeatureType TYPE = b.buildFeatureType();


        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection("internal",TYPE);
        List<LineString> lineStrings = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for(StreetSegment segment : segments){
            LineString lineString = geometryFactory.createLineString(segment.getCoordinates());
            lineStrings.add(lineString);
            ids.add(segment.id);
        }

        SimpleFeature[] simpleFeatures = features(TYPE, lineStrings.toArray(new LineString[]{}), ids.toArray(new Long[]{}));

        for(int i = 0; i < simpleFeatures.length; i++){
            featureCollection.add(simpleFeatures[i]);
        }

        File newFile = new File(filePath + "/opentraffic_route.shp");

        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", newFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(TYPE);

        /*
         * You can comment out this line if you are using the createFeatureType method (at end of
         * class file) rather than DataUtilities.createType
         */
        newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);

        Transaction transaction = new DefaultTransaction("create");

        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = newDataStore.getFeatureSource(typeName);

        if (featureSource instanceof SimpleFeatureStore) {
            SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;

            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(featureCollection);
                transaction.commit();

            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();

            } finally {
                transaction.close();
            }
        } else {
            System.out.println(typeName + " does not support read/write access");
        }
    }

    public static SimpleFeature[] features( SimpleFeatureType schema, LineString[] lines, Long[] ids ) throws SchemaException, IllegalAttributeException {

        SimpleFeature[] features = new SimpleFeature[ lines.length ];

        for ( int i = 0; i < lines.length; i++) {
            features[i] = SimpleFeatureBuilder.build(schema, new Object[] {lines[i], ids[i]}, "fid" + ids[i].toString());
        }

        return features;
    }

}
