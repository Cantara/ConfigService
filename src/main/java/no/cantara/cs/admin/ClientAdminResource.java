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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
    //clientName which matches one of these elements will have a new name [ComputerName - local_ip - wrappedod/os 
    private final String[] defaultClientNameList = {"Default clientName", "Default client", "local-jau"};

    @Autowired
    public ClientAdminResource(EventsDao eventsDao, ClientDao clientDao, ClientService clientService) {
        this.eventsDao = eventsDao;
        this.clientDao = clientDao;
        this.clientService = clientService;
    }


    //TODO no test?
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
    
    //ChangeConfigForSpecificClientTest.testChangeConfigForSingleClient
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
    @Path("/{clientId}/heartbeat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientHeartbeat(@Context SecurityContext context, @PathParam("clientId") String clientId) {
        log.trace("getStatus");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ClientHeartbeatData clientHeartbeatData = clientDao.getClientHeartbeatData(clientId);

        return mapResponseToJson(clientHeartbeatData);
    }
    
    //ClientAdminResourceStatusTest
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
        ClientEnvironment clientEnv = clientDao.getClientEnvironment(clientId);
        if(clientEnv!=null && clientHeartbeatData!=null) {
        	if(Arrays.asList(defaultClientNameList).contains(clientHeartbeatData.clientName)) {
				clientHeartbeatData.clientName = makeUpADefaultClientName(clientEnv);
			}
        }
        ClientStatus statusView = new ClientStatus(client, clientHeartbeatData);
        return mapResponseToJson(statusView);
    }
    
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllClientStatuses(@Context SecurityContext context) {
        log.trace("getStatus");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<ClientStatus> clientStatuses = new ArrayList<>();
        List<Client> clientList = clientDao.getAllClients();
        List<String> ignoredList = clientDao.getAllIgnoredClientIds();
		Map<String, ClientHeartbeatData> allclientHeartbeatData = clientDao.getAllClientHeartbeatData();
		Map<String, ClientEnvironment> allclientEnvs = clientDao.getAllClientEnvironments();
		
        for(Client client : clientList) {
        	if(!ignoredList.contains(client.clientId)) {
        
        		ClientHeartbeatData clientHeartbeatData = allclientHeartbeatData.get(client.clientId);
        		ClientEnvironment clientEnv = allclientEnvs.get(client.clientId);
        		
        		if(clientEnv!=null && clientHeartbeatData!=null) {
        			if(Arrays.asList(defaultClientNameList).contains(clientHeartbeatData.clientName)) {
        				clientHeartbeatData.clientName = makeUpADefaultClientName(clientEnv);
        			}
        		}
        		
        		ClientStatus statusView = new ClientStatus(client, clientHeartbeatData);
        		clientStatuses.add(statusView);
        	}
        }       
        return mapResponseToJson(clientStatuses);
    }
    
    
    private String makeUpADefaultClientName(ClientEnvironment env) {
		String computerName = env.envInfo.get("COMPUTERNAME");
		String localIP = "";
		for(String key : env.envInfo.keySet()) {
			if(key.startsWith("networkinterface_")) {
				localIP = env.envInfo.get(key);
				break;
			}
		}
		String wrapped_os = env.envInfo.containsKey("WRAPPER_OS")? 
				env.envInfo.get("WRAPPER_OS"): env.envInfo.get("OS");
		return (computerName!=null? (computerName + " - "):"") + localIP + (wrapped_os!=null? (" - " + wrapped_os) : "");
	}


    ////ClientAdminResourceEnvTest
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

    //TODO no test?
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

    //TODO no test?
    @GET
    @Path("/{clientId}/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicationConfigForClient(@Context SecurityContext context, @PathParam("clientId") String clientId) {
        log.trace("Invoked getApplicationConfigForClient");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ApplicationConfig config = clientService.findApplicationConfigByClientId(clientId);
        if (config == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return mapResponseToJson(config);
    }

    //ChangeConfigForSpecificClientTest.testChangeConfigForSingleClient, RegisterClientWithPreconfiguredConfigTest.testRegisterClientWithPreconfiguredConfig
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
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .build();
        }
    }
    
    @GET
    @Path("/aliases")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllClientAlliases(@Context SecurityContext context) {
        log.trace("getAllClientAlliases");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<ClientAlias> allClients = clientDao.getAllClientAliases();
        return mapResponseToJson(allClients);
    }

    @PUT
    @Path("/aliases/{clientId}/{clientName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response putClientAlias(@Context SecurityContext context, @PathParam("clientId") String clientId, @PathParam("clientName")  String clientName) {
        log.debug("Invoked putClientAlias clientId={}/clientName={}", clientId, clientName);
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        ClientAlias updatedClientAlias = new ClientAlias(clientId, clientName);

        try {
            clientDao.saveClientAlias(updatedClientAlias);

            return mapResponseToJson(updatedClientAlias);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/env")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllClientEnvironments(@Context SecurityContext context) {
        log.trace("getAllClientEnvironments");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Map<String, ClientEnvironment> allClientEnvs = clientDao.getAllClientEnvironments();
        return mapResponseToJson(allClientEnvs);
    }
    
    @GET
    @Path("/heartbeats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllClientHeartbeatData(@Context SecurityContext context) {
        log.trace("getAllClientHeartbeatData");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Map<String, ClientHeartbeatData> allClientHeartBeats = clientDao.getAllClientHeartbeatData();
        return mapResponseToJson(allClientHeartBeats);
    }
   
    @GET
    @Path("/ignoredClients")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllIgnoredClientIds(@Context SecurityContext context) {
        log.trace("getAllIgnoredClientIds");
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<String> allClients = clientDao.getAllIgnoredClientIds();
        return mapResponseToJson(allClients);
    } 
    
    @PUT
    @Path("/ignoredClients/{clientId}/{flag}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putIgnoreAClient(@Context SecurityContext context, @PathParam("clientId") String clientId, @PathParam("flag") String flag) {
        log.debug("Invoked putClientAlias clientId={}/set ignored={}", clientId, flag.equals("1") || flag.equals("true"));
        if (!isAdmin(context)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        clientDao.saveIgnoredFlag(clientId, flag.equals("1") || flag.equals("true"));
        try {
            return mapResponseToJson(clientId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .build();
        }
    }

    private Response mapResponseToJson(Object response) {
       
        try {
        	if(response!=null) {
        		String jsonResult="";
        		jsonResult = mapper.writeValueAsString(response);
                return Response.ok(jsonResult).build();
        	}
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", response.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    private boolean isAdmin(@Context SecurityContext context) {
        return context.isUserInRole(Main.ADMIN_ROLE);
    }

}
