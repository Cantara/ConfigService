package no.cantara.cs.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.cs.Main;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ClientService;
import no.cantara.cs.dto.*;
import no.cantara.cs.dto.event.ExtractedEventsStore;
import no.cantara.cs.persistence.ClientDao;
import no.cantara.cs.persistence.EventsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.List;

/**
 * Http endpoints for administration APIs for client: https://wiki.cantara.no/display/JAU/ConfigService+Admin+API
 *
 * Created by jorunfa on 04/11/15.
 */
@Path(ClientResource.CLIENT_PATH)
public class ClientAdminResource {
    private static final Logger log = LoggerFactory.getLogger(ClientAdminResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final EventsDao eventsDao;
    private final ClientDao clientDao;
    private final ClientService clientService;

    @Autowired
    public ClientAdminResource(EventsDao eventsDao, ClientDao clientDao, ClientService clientService) {
        this.eventsDao = eventsDao;
        this.clientDao = clientDao;
        this.clientService = clientService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllClients(@Context SecurityContext context) {
        log.trace("getAllClients");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<Client> allClients = clientDao.getAllClients();
        return mapResponseToJson(allClients);
    }

    @GET
    @Path("/{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClient(@Context SecurityContext context, @PathParam("clientId") String clientId) {
        log.trace("getClient");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Client client = clientDao.getClient(clientId);
        if (client == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return mapResponseToJson(client);
    }

    @GET
    @Path("/{clientId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus(@Context SecurityContext context, @PathParam("clientId") String clientId) {
        log.trace("getStatus");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Client client = clientDao.getClient(clientId);
        if (client == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        ClientHeartbeatData clientHeartbeatData = clientDao.getClientHeartbeatData(clientId);

        ClientStatus statusView = new ClientStatus(client, clientHeartbeatData);
        return mapResponseToJson(statusView);
    }

    @GET
    @Path("/{clientId}/env")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEnvironment(@Context SecurityContext context, @PathParam("clientId") String clientId) {
        log.trace("getStatus");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ClientEnvironment clientEnvironment = clientDao.getClientEnvironment(clientId);
        if (clientEnvironment == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return mapResponseToJson(clientEnvironment);
    }

    @GET
    @Path("/{clientId}/events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientEvents(@Context SecurityContext context, @PathParam("clientId") String clientId) {
        log.trace("getClientEvents");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ExtractedEventsStore store = eventsDao.getEvents(clientId);
        return mapResponseToJson(store);
    }

    @GET
    @Path("/{clientId}/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientConfig(@Context SecurityContext context, @PathParam("clientId") String clientId) {
        log.trace("Invoked getClientConfig");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ApplicationConfig config = clientService.findApplicationConfigByClientId(clientId);
        if (config == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return mapResponseToJson(config);
    }

    @PUT
    @Path("/{clientId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putClient(@Context SecurityContext context, @PathParam("clientId") String clientId, String jsonRequest) {
        log.debug("Invoked updateClient clientId={} with request {}", clientId, jsonRequest);
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Client client;
        try {
            client = mapper.readValue(jsonRequest, Client.class);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), jsonRequest);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        if (!clientId.equals(client.clientId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Path parameter clientId '" + clientId +
                    "' does not match body clientId: '" + client.clientId + "'").build();
        }

        Client updatedClient = new Client(client.clientId, client.applicationConfigId, client.autoUpgrade);

        try {
            clientDao.saveClient(updatedClient);

            return mapResponseToJson(updatedClient);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .build();
        }
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

    private boolean isAdmin(@Context SecurityContext context) {
        return context.isUserInRole(Main.ADMIN_ROLE);
    }
}
