package actors;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.conveyal.traffic.geom.GPSPoint;

import controllers.Application;
import play.Logger;
import play.Play;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
 
public class PurgeActor extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	
	public void onReceive(Object message) throws Exception {
		
		// purge vehicles not heard from for at least 5 minutes
		//Long purgedVehicles = Application.engine.te.purgeVehicles(5 * 60);
		
		//log.info("Purged " + purgedVehicles + " vehicles, " + Application.engine.te.getVehicleCount() + " active." );
		
	}

}