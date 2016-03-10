package no.cantara.cs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.cs.persistence.EventsDao;
import no.cantara.cs.persistence.ConfigDao;
import no.cantara.cs.persistence.StatusDao;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.Config;
import no.cantara.cs.dto.event.ExtractedEventsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Http endpoint for ClientStatus
 *
 * Created by jorunfa on 04/11/15.
 */
@Path(ClientResource.CLIENT_PATH)
public class ClientResource {

    public static final String CLIENT_PATH = "/client";
    private static final Logger log = LoggerFactory.getLogger(ClientResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final StatusDao statusDao;
    private final EventsDao eventsDao;
    private final ConfigDao configDao;

    @Autowired
    public ClientResource(ConfigDao configDao,
                          StatusDao statusDao,
                          EventsDao eventsDao) {
        this.configDao = configDao;
        this.statusDao = statusDao;
        this.eventsDao = eventsDao;
    }

    @GET
    @Path("/{clientId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusForAll(@PathParam("clientId") String clientId) {
        log.trace("getStatusForAll");
        ClientStatus status = statusDao.getStatus(clientId);

        return mapResponseToJson(status);
    }

    @GET
    @Path("/{clientId}/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientEvents(@PathParam("clientId") String clientId) {
        log.trace("getClientEvents");
        ExtractedEventsStore store = eventsDao.getEvents(clientId);
        return mapResponseToJson(store);
    }

    @GET
    @Path("/{clientId}/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientConfig(@PathParam("clientId") String clientId) {
        log.trace("Invoked getClientConfig");
        Config config = configDao.findByClientId(clientId);

        if (config == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return mapResponseToJson(config);
    }

    @PUT
    @Path("/{clientId}/config/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateClientConfig(@PathParam("clientId") String clientId,
                                                        String clientConfigAsJson) {
        log.debug("Invoked updateClientConfig clientId={} with json {}", clientId,
                clientConfigAsJson);

        ClientConfig newClientConfig;
        try {
            newClientConfig = mapper.readValue(clientConfigAsJson, ClientConfig.class);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), clientConfigAsJson);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        String configId = newClientConfig.config.getId();

        if (configDao.findByClientId(clientId) == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Client with id '" +
                    clientId + "' is not registered.")
                    .build();
        }

        Config oldConfig = configDao.changeConfigForClientToUse(clientId, configId);

        if (oldConfig == null) {
            log.trace("Config with id '" + configId + "' does not exist. Config for client '"
                    + clientId + "' not updated.");
            return Response.status(Response.Status.BAD_REQUEST).entity("Config with id '" +
                    configId + "' does not exist. Config for clientId '" + clientId + "' not updated.")
                    .build();
        }

        return mapResponseToJson(oldConfig);
    }

    private Response mapResponseToJson(Object response) {
        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(response);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", response.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(jsonResult).build();
    }

}
