package no.cantara.cs.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Simple health endpoint for checking the server is running
 *
 * @author <a href="mailto:asbjornwillersrud@gmail.com">Asbjørn Willersrud</a> 30/03/2016.
 */
@Path(HealthResource.HEALTH_PATH)
public class HealthResource {

    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    public static final String HEALTH_PATH = "/health";

    @GET
    @Path("/")
    public Response healthCheck() {
        log.trace("healthCheck");
        return Response.ok().build();
    }

}
