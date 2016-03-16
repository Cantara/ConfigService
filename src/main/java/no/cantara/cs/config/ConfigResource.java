package no.cantara.cs.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.cs.application.ApplicationResource;
import no.cantara.cs.persistence.ConfigDao;
import no.cantara.cs.dto.Config;
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
@Path(ConfigResource.CONFIG_PATH)
public class ConfigResource {
    public static final String CONFIG_PATH = ApplicationResource.APPLICATION_PATH + "/{applicationId}/config";

    private static final Logger log = LoggerFactory.getLogger(ConfigResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ConfigDao configDao;

    @Autowired
    public ConfigResource(ConfigDao configDao) {
        this.configDao = configDao;
    }

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createConfig(@PathParam("applicationId") String applicationId, String json) {
    	log.debug("Invoked createConfig with json {} and applicationId {}", json, applicationId);

        Config newConfig;
        try {
            newConfig = mapper.readValue(json, Config.class);
        } catch (IOException e) {
            Response.Status status = Response.Status.BAD_REQUEST;
            log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
            return Response.status(status).build();
        }

        try {
            Config persistedConfig = configDao.createConfig(applicationId, newConfig);
            log.info("created {}", persistedConfig);
            String jsonResult = mapper.writeValueAsString(persistedConfig);
            return Response.ok(jsonResult).build();
        } catch (IOException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PUT
    @Path("/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateConfig(@PathParam("configId") String configId, String json) {
    	log.debug("Invoked updateConfig with json {}", json);

        Config updatedConfig;
        try {
            updatedConfig = mapper.readValue(json, Config.class);
        } catch (IOException e) {
            Response.Status status = Response.Status.BAD_REQUEST;
            log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
            return Response.status(status).build();
        }

        Config persistedUpdatedConfig = configDao.updateConfig(updatedConfig);
        if (persistedUpdatedConfig == null) {
            log.warn("Could not update Config with json={}", json);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(updatedConfig);
        } catch (JsonProcessingException e) {
            log.error("{}", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllConfigs() {
        log.trace("invoked getAllConfigs");

        Map<String, Config> allConfigs = configDao.getAllConfigs();

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(allConfigs);
        } catch (JsonProcessingException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }

    // example http://localhost:8086/jau/application/ad4911cf-9e1a-4307-bacc-3de0a2aae679/serviceconfig/fd5cf7d8-4b6e-445b-9fbc-b468fcc44014
    @GET
    @Path("/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfig(@PathParam("configId") String configId) {
        log.trace("getConfig with configId={}", configId);

        Config config = configDao.getConfig(configId);
        if (config == null) {
            log.warn("Could not find Config with id={}", configId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResult).build();
    }

    @DELETE
    @Path("/{configId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteConfig(@PathParam("configId") String configId) {
        log.debug("deleteConfig with configId={}", configId);

        Config Config = configDao.deleteConfig(configId);
        if (Config == null) {
            log.warn("Could not find and therefore not delete Config with id={}", configId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
