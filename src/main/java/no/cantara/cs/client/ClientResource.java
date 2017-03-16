package no.cantara.cs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.cs.config.ConstrettoConfig;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * HTTP endpoints for client API: https://wiki.cantara.no/display/JAU/ConfigService+Client+API
 *
 * Created by jorunfa on 04/11/15.
 */
@Path(ClientResource.CLIENT_PATH)
public class ClientResource {
    public static final String CLIENT_PATH = "/client";
    private static final Logger log = LoggerFactory.getLogger(ClientResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ClientService clientService;
    private final boolean clientSecretValidationEnabled;

    @Autowired
    public ClientResource(ClientService clientService) {
        this.clientService = clientService;
        this.clientSecretValidationEnabled = ConstrettoConfig.getBoolean("client.secret.validation.enabled");
    }

    @POST
    @Path("/registration")
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
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        if (clientConfig == null) {
            log.debug("Returning 404, not found");
            return Response.status(Response.Status.NOT_FOUND).entity("Valid request, but no matching ApplicationConfig could be found. Probably missing data on server.").build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(clientConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", clientConfig.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        log.info("registered {}", clientConfig);
        return Response.ok(jsonResult).build();
    }

    @POST
    @Path("/{clientId}/sync")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response sync(@PathParam("clientId") String clientId, String checkForUpdateRequestJson) {
        log.trace("checkForUpdate with clientId={}", clientId);

        CheckForUpdateRequest checkForUpdateRequest;
        try {
            checkForUpdateRequest = fromJson(checkForUpdateRequestJson);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), checkForUpdateRequestJson);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        if (clientSecretValidationEnabled && !clientService.validateClientSecret(checkForUpdateRequest)) {
            log.warn("Invalid clientSecret={} for clientId={}. Returning {}",
                    checkForUpdateRequest.clientSecret, checkForUpdateRequest.clientId, Response.Status.UNAUTHORIZED);
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or mismatching client id and/or secret.").build();
        }

        clientService.processEvents(clientId, checkForUpdateRequest.eventsStore);


        ClientConfig clientConfig = clientService.checkForUpdatedClientConfig(clientId, checkForUpdateRequest);
        if (clientConfig == null) {
            String msg = "No ClientConfig could be found. Not registered? clientId=" + clientId + ", configLastChanged=" + checkForUpdateRequest.configLastChanged;
            log.debug(msg);
            return Response.status(Response.Status.PRECONDITION_FAILED).entity(msg).build();
        }

        if (clientConfig.config == null) {
            log.trace("ClientConfig has not changed, return 204 No Content configLastChanged={}", checkForUpdateRequest.configLastChanged);
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(clientConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", clientConfig.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        log.info("New ClientConfig found. clientId={}, configLastChangedServer={}, configLastChangedFromClient={}",
                clientId, clientConfig.config.getLastChanged(), checkForUpdateRequest.configLastChanged);
        return Response.ok(jsonResult).build();
    }

    static CheckForUpdateRequest fromJson(String checkForUpdateRequestJson) throws IOException {
        return mapper.readValue(checkForUpdateRequestJson, CheckForUpdateRequest.class);
    }
}
