package no.cantara.jau.serviceconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.persistence.ServiceConfigDao;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

/**
 * CRUD, http endpoint for ServiceConfig
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path(ServiceConfigResource.SERVICECONFIG_PATH)
public class ServiceConfigResource {
    public static final String SERVICECONFIG_PATH = ApplicationResource.APPLICATION_PATH + "/{applicationId}/serviceconfig";

    private static final Logger log = LoggerFactory.getLogger(ServiceConfigResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ServiceConfigDao serviceConfigDao;

    @Autowired
    public ServiceConfigResource(ServiceConfigDao serviceConfigDao) {
        this.serviceConfigDao = serviceConfigDao;
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createServiceConfig(@PathParam("applicationId") String applicationId, String json) {
    	log.debug("Invoked createServiceConfig with json {} and applicationId {}", json, applicationId);

        ServiceConfig newServiceConfig;
        try {
            newServiceConfig = mapper.readValue(json, ServiceConfig.class);
        } catch (IOException e) {
            Response.Status status = Response.Status.BAD_REQUEST;
            log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
            return Response.status(status).build();
        }

        try {
            ServiceConfig persistedServiceConfig = serviceConfigDao.createServiceConfig(applicationId, newServiceConfig);
            log.info("created {}", persistedServiceConfig);
            String jsonResult = mapper.writeValueAsString(persistedServiceConfig);
            return Response.ok(jsonResult).build();
        } catch (IOException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{serviceConfigId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateServiceConfig(@PathParam("serviceConfigId") String serviceConfigId, String json) {
    	log.debug("Invoked updateServiceConfig with json {}", json);

        ServiceConfig updatedServiceConfig;
        try {
            updatedServiceConfig = mapper.readValue(json, ServiceConfig.class);
        } catch (IOException e) {
            Response.Status status = Response.Status.BAD_REQUEST;
            log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
            return Response.status(status).build();
        }

        ServiceConfig persistedUpdatedServiceConfig = serviceConfigDao.updateServiceConfig(updatedServiceConfig);
        if (persistedUpdatedServiceConfig == null) {
            log.warn("Could not update serviceConfig with json={}", json);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(updatedServiceConfig);
        } catch (JsonProcessingException e) {
            log.error("{}", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllServiceConfigs() {
        log.trace("invoked getAllServiceConfigs");

        Map<String, ServiceConfig> serviceConfig = serviceConfigDao.getAllServiceConfigs();

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(serviceConfig);
        } catch (JsonProcessingException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }

    // example http://localhost:8086/jau/application/ad4911cf-9e1a-4307-bacc-3de0a2aae679/serviceconfig/fd5cf7d8-4b6e-445b-9fbc-b468fcc44014
    @GET
    @Path("/{serviceConfigId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServiceConfig(@PathParam("serviceConfigId") String serviceConfigId) {
        log.trace("getServiceConfig with serviceConfigId={}", serviceConfigId);

        ServiceConfig serviceConfig = serviceConfigDao.getServiceConfig(serviceConfigId);
        if (serviceConfig == null) {
            log.warn("Could not find serviceConfig with id={}", serviceConfigId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(serviceConfig);
        } catch (JsonProcessingException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }

    @DELETE
    @Path("/{serviceConfigId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteServiceConfig(@PathParam("serviceConfigId") String serviceConfigId) {
        log.debug("deleteServiceConfig with serviceConfigId={}", serviceConfigId);

        ServiceConfig serviceConfig = serviceConfigDao.deleteServiceConfig(serviceConfigId);
        if (serviceConfig == null) {
            log.warn("Could not find and therefore not delete serviceConfig with id={}", serviceConfigId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
