package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import play.mvc.Controller;
import play.mvc.Result;
import routing.Routing;

import java.awt.*;
//import java.time.LocalDateTime;
//import java.time.OffsetDateTime;
//import java.time.ZoneOffset;
//import java.time.temporal.ChronoField;

/**
 * Controller to do routing.
 */
public class RoutingController extends Controller {
    /** for starters one graph of the whole world */
    private static Routing routing = new Routing(new Rectangle(-180, -90, 360, 180));

    private static ObjectMapper mapper = new ObjectMapper();

    public static Result plan(double fromLat, double fromLon, double toLat, double toLon, int day, int time, boolean useTraffic) {
        routing.buildIfUnbuilt();

        RoutingRequest rr = new RoutingRequest();

     //   rr.useTraffic = useTraffic;
        rr.from = new GenericLocation(fromLat, fromLon);
        rr.to = new GenericLocation(toLat, toLon);

        // figure out the time
       // LocalDateTime dt = LocalDateTime.now();
       // dt = dt.with(ChronoField.DAY_OF_WEEK, day).withHour(0).withMinute(0).withSecond(0);
       // dt = dt.plusSeconds(time);
       // rr.dateTime = OffsetDateTime.of(dt, ZoneOffset.UTC).toEpochSecond();

        TripPlan tp = routing.route(rr);

        return json(tp);
    }

    private static Result json (Object obj) {
        try {
            return ok(mapper.writeValueAsString(obj)).as("application/json");
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return internalServerError();
        }
    }
}
