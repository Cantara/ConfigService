package no.cantara.jau.clientconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Http endpoint for ClientConfig
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path("/clientconfig")
public class ClientConfigResource {
    private static final Logger log = LoggerFactory.getLogger(ClientConfigResource.class);

    @Autowired
    public ClientConfigResource() {
    }

    //https://github.com/Cantara/Java-Auto-Update/issues/8
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerClient(String json) {
        log.trace("registerClient");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    //https://github.com/Cantara/Java-Auto-Update/issues/9
    @POST
    @Path("/{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response syncClientConfig(@PathParam("clientid") String clientid) {
        log.trace("syncClientConfig with clientid={}", clientid);
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
