package no.cantara.jau.clientconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.serviceconfig.dto.CheckForUpdateRequest;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Http endpoint for ClientConfig
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path(ClientConfigResource.CLIENTCONFIG_PATH)
public class ClientConfigResource {
    public static final String CLIENTCONFIG_PATH = "/clientconfig";
    private static final Logger log = LoggerFactory.getLogger(ClientConfigResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
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

        //Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        //String artifactId =  JsonPath.read(document, "$.artifactId");
        ClientRegistrationRequest registration;
        try {
            registration = mapper.readValue(json, ClientRegistrationRequest.class);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), json);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        ClientConfig clientConfig;
        try {
            clientConfig = configSearcher.registerClient(registration);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Not enough information to register client.").build();
        }
        if (clientConfig == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Valid request, but no matching ServiceConfig could be found. Probably missing data on server.").build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(clientConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", clientConfig.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build(); //Switch to 201 created and set url
    }

    //https://github.com/Cantara/Java-Auto-Update/issues/9
    @POST
    @Path("/{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkForUpdate(@PathParam("clientId") String clientId, String json) {
        log.trace("checkForUpdates with clientId={}", clientId);
        CheckForUpdateRequest body;
        try {
            body = mapper.readValue(json, CheckForUpdateRequest.class);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), json);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        ClientConfig clientConfig = configSearcher.getClientConfig(clientId, body.checksum);
        //TODO return 204 No content if checksum is the same

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(clientConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", clientConfig.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }
}
