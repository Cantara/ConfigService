package no.cantara.jau.clientconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.apache.regexp.internal.RE;
import no.cantara.jau.persistence.ServiceConfigDao;
import no.cantara.jau.persistence.StatusDao;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.ws.Service;
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
    private final ServiceConfigDao configDao;

    @Autowired
    public ClientResource(ServiceConfigDao configDao,
                          StatusDao statusDao) {
        this.configDao = configDao;
        this.statusDao = statusDao;
    }

    @GET
    @Path("/{clientId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusForAll(@PathParam("clientId") String clientId) {
        log.trace("getStatusForAll");
        ClientStatus status = statusDao.getStatus(clientId);

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(status);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", status.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(jsonResult).build();
    }

    @GET
    @Path("/{clientId}/config")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClientConfig(@PathParam("clientId") String clientId) {
        log.trace("Invoked getClientConfig");
        ServiceConfig serviceConfig = configDao.findByClientId(clientId);

        if (serviceConfig == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(serviceConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", serviceConfig.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(jsonResult).build();
    }

    @PUT
    @Path("/{clientId}/config/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateClientConfig(@PathParam("clientId") String clientId,
                                                        String clientConfigAsJson) {
        log.trace("Invoked updateClientConfig clientId={} with json {}", clientId,
                clientConfigAsJson);

        ClientConfig newClientConfig;
        try {
            newClientConfig = mapper.readValue(clientConfigAsJson, ClientConfig.class);
        } catch (IOException e) {
            log.error("Error parsing json. {}, json={}", e.getMessage(), clientConfigAsJson);
            return Response.status(Response.Status.BAD_REQUEST).entity("Could not parse json.").build();
        }

        ServiceConfig oldServiceConfig = configDao.changeServiceConfigForClientToUse(clientId,
                newClientConfig.serviceConfig.getId());

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(oldServiceConfig);
        } catch (JsonProcessingException e) {
            log.error("{}", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }

}
