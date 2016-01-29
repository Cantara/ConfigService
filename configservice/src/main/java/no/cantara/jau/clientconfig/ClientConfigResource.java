package no.cantara.jau.clientconfig;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import net.minidev.json.JSONArray;
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
    private final ClientService clientService;

    @Autowired
    public ClientConfigResource(ClientService clientService) {
        this.clientService = clientService;
    }

    //https://github.com/Cantara/Java-Auto-Update/issues/8
    //required parameters: artifactId
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerClient(String json) {
        log.trace("registerClient");
        //Should probably use jsonpath
        ClientRegistrationRequest registration;
        try {
            registration = mapper.readValue(json, ClientRegistrationRequest.class);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), json);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        ClientConfig clientConfig;
        try {
            clientConfig = clientService.registerClient(registration);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Not enough information to register client.").build();
        }
        if (clientConfig == null) {
            log.debug("Returning 404, not found");
            return Response.status(Response.Status.NOT_FOUND).entity("Valid request, but no matching ServiceConfig could be found. Probably missing data on server.").build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(clientConfig);

            removeExtractionConfigForOldJau(registration, jsonResult);

        } catch (IOException e) {
            log.warn("Could not convert to Json {}", clientConfig.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        log.info("registered {}", clientConfig);
        return Response.ok(jsonResult).build();
    }

    private void removeExtractionConfigForOldJau(ClientRegistrationRequest registration, String jsonResult) throws IOException {
        String jauVersion = registration.envInfo.get("jau.version");
        if ("0.4.1".equals(jauVersion)) {
            ObjectNode jsonNode = (ObjectNode) mapper.readTree(jsonResult);
            ObjectNode serviceConfigNode = (ObjectNode) jsonNode.get("serviceConfig");
            serviceConfigNode.remove("eventExtractionConfigs");
        }
    }

    //https://github.com/Cantara/Java-Auto-Update/issues/9
    @POST
    @Path("/{clientId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkForUpdate(@PathParam("clientId") String clientId, String json) {
        log.trace("checkForUpdate with clientId={}", clientId);
        CheckForUpdateRequest checkForUpdateRequest;
        try {
            checkForUpdateRequest = mapper.readValue(json, CheckForUpdateRequest.class);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), json);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        ClientConfig newClientConfig = clientService.checkForUpdatedClientConfig(clientId, checkForUpdateRequest);
        if (newClientConfig == null) {
            String msg = "No ClientConfig could be found. Not registered? clientId=" + clientId + ", serviceConfigLastChanged=" + checkForUpdateRequest.serviceConfigLastChanged;
            log.debug(msg);
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(msg).build();
        }

        if (newClientConfig.serviceConfig.getLastChanged().equals(checkForUpdateRequest.serviceConfigLastChanged)) {
            log.trace("ClientConfig has not changed, return 204 No Content serviceConfigLastChanged={}", checkForUpdateRequest.serviceConfigLastChanged);
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(newClientConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", newClientConfig.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        log.info("New ClientConfig found. clientId={}, serviceConfigLastChangedServer={}, serviceConfigLastChangedFromClient={}",
                clientId, newClientConfig.serviceConfig.getLastChanged(), checkForUpdateRequest.serviceConfigLastChanged);
        return Response.ok(jsonResult).build();
    }
}
