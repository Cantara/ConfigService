package no.cantara.cs.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.cantara.cs.application.ApplicationResource;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.persistence.ApplicationConfigDao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Map;

/**
 * CRUD, http endpoint for ApplicationConfig
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path(ApplicationResource.APPLICATION_PATH)
public class ApplicationConfigResource {
	public static final String CONFIG_PATH = "/{applicationId}/config";

	private static final Logger log = LoggerFactory.getLogger(ApplicationConfigResource.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	private final ApplicationConfigDao applicationConfigDao;

	@Autowired
	public ApplicationConfigResource(ApplicationConfigDao applicationConfigDao) {
		this.applicationConfigDao = applicationConfigDao;
	}

	@POST
	@Path(CONFIG_PATH)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createConfig(@PathParam("applicationId") String applicationId, String json) {
		log.debug("Invoked createConfig with json {} and applicationId {}", json, applicationId);

		ApplicationConfig newConfig;
		try {
			newConfig = mapper.readValue(json, ApplicationConfig.class);
		} catch (IOException e) {
			Response.Status status = Response.Status.BAD_REQUEST;
			log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
			return Response.status(status).build();
		}

		try {
			ApplicationConfig persistedConfig = applicationConfigDao.createApplicationConfig(applicationId, newConfig);
			log.info("created {}", persistedConfig);
			String jsonResult = mapper.writeValueAsString(persistedConfig);
			return Response.ok(jsonResult).build();
		} catch (IOException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PUT
	@Path(CONFIG_PATH + "/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateConfig(@PathParam("configId") String configId, String json) {
		log.debug("Invoked updateConfig with json {}", json);

		ApplicationConfig updatedConfig;
		try {
			updatedConfig = mapper.readValue(json, ApplicationConfig.class);
		} catch (IOException e) {
			Response.Status status = Response.Status.BAD_REQUEST;
			log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
			return Response.status(status).build();
		}

		ApplicationConfig persistedUpdatedConfig = applicationConfigDao.updateApplicationConfig(configId, updatedConfig);
		if (persistedUpdatedConfig == null) {
			log.warn("Could not update ApplicationConfig with json={}", json);
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
	@Path("/config")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllConfigs() {
		log.trace("invoked getAllConfigs");

		Map<String, ApplicationConfig> allConfigs = applicationConfigDao.getAllConfigs();

		String jsonResult;
		try {
			jsonResult = mapper.writeValueAsString(allConfigs);
		} catch (JsonProcessingException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		return Response.ok(jsonResult).build();
	}

	@GET
	@Path(CONFIG_PATH)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigForApplication(@PathParam("applicationId") String applicationId) {
		log.trace("invoked getConfigForApplication");

		ApplicationConfig applicationConfig = applicationConfigDao.findApplicationConfigByApplicationId(applicationId);
		if (applicationConfig == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		String jsonResult;
		try {
			jsonResult = mapper.writeValueAsString(applicationConfig);
		} catch (JsonProcessingException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		return Response.ok(jsonResult).build();
	}

	@GET
	@Path(CONFIG_PATH + "/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfig(@PathParam("configId") String configId) {
		log.trace("getConfig with configId={}", configId);

		ApplicationConfig config = applicationConfigDao.getApplicationConfig(configId);
		if (config == null) {
			log.warn("Could not find ApplicationConfig with id={}", configId);
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
	@Path(CONFIG_PATH + "/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteConfig(@PathParam("configId") String configId) {
		log.debug("deleteConfig with configId={}", configId);

		if(applicationConfigDao.canDeleteApplicationConfig(configId)){
			ApplicationConfig config = applicationConfigDao.deleteApplicationConfig(configId);
			if (config == null) {
				log.warn("Could not find and therefore not delete ApplicationConfig with id={}", configId);
				return Response.status(Response.Status.NOT_FOUND).build();
			}
		} else {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}
		return Response.status(Response.Status.NO_CONTENT).build();
	}
	
	@DELETE
	@Path("/{applicationId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteApplication(@PathParam("applicationId") String applicationId) {
		log.debug("delete application ={}", applicationId);

		if(applicationConfigDao.canDeleteApplication(applicationId)){
			Application app = applicationConfigDao.deleteApplication(applicationId);
			if (app == null) {
				log.warn("Could not find and therefore not delete ApplicationConfig with id={}", app);
				return Response.status(Response.Status.NOT_FOUND).build();
			}
		} else {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}
		return Response.status(Response.Status.NO_CONTENT).build();
	}

}
