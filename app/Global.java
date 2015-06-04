import java.util.concurrent.TimeUnit;

import actors.PurgeActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import play.*;
import play.libs.Akka;
import scala.concurrent.duration.Duration;

public class Global extends GlobalSettings {

    public void onStart(Application app) {
    	
    	//ActorRef ingestActor = Akka.system().actorOf(Props.create(IngestActor.class));
    	
    	//Akka.system().scheduler().schedule(Duration.Zero(),
    	//		Duration.create(1, TimeUnit.MINUTES), ingestActor, "Ingest", Akka.system().dispatcher(), null);
    
    	//ActorRef purgeActor = Akka.system().actorOf(Props.create(PurgeActor.class));
    	
    	//Akka.system().scheduler().schedule(Duration.Zero(),
    	//		Duration.create(30, TimeUnit.SECONDS), purgeActor, "Purge", Akka.system().dispatcher(), null);
    
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }

}