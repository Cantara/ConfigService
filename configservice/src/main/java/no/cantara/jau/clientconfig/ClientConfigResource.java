package no.cantara.jau.clientconfig;

import no.cantara.jau.persistence.ConfigSearcher;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientConfigSerializer;
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
    private final ConfigSearcher configSearcher;

    @Autowired
    public ClientConfigResource(ConfigSearcher configSearcher) {
        this.configSearcher = configSearcher;
    }

    //https://github.com/Cantara/Java-Auto-Update/issues/8
    //required parameters: artifactId
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerClient(String json) {
        log.trace("registerClient");
        ClientConfig clientConfig;
        try {
            clientConfig = configSearcher.registerClient(json);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Not enough information to register client.").build();
        }
        if (clientConfig == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Valid request, but no matching ServiceConfig could be found. Probably missing data on server.").build();
        }

        String jsonResult = ClientConfigSerializer.toJson(clientConfig);
        return Response.ok(jsonResult).build();
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
