package no.cantara.cs.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.persistence.ApplicationConfigDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
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
	public Response createApplicationConfig(@PathParam("applicationId") String applicationId, String json) {
		log.debug("Invoked createApplicationConfig with json {} and applicationId {}", json, applicationId);

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
	@Path(CONFIG_PATH + "/{applicationConfigId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateApplicationConfig(@PathParam("applicationConfigId") String applicationConfigId, String json) {
		log.debug("Invoked updateApplicationConfig with json {}", json);

		ApplicationConfig updatedConfig;
		try {
			updatedConfig = mapper.readValue(json, ApplicationConfig.class);
		} catch (IOException e) {
			Response.Status status = Response.Status.BAD_REQUEST;
			log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
			return Response.status(status).build();
		}

		ApplicationConfig persistedUpdatedConfig = applicationConfigDao.updateApplicationConfig(applicationConfigId, updatedConfig);
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
	public Response getAllApplicationConfigs() {
		log.trace("invoked getAllApplicationConfigs");

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
	public Response getApplicationConfigForApplication(@PathParam("applicationId") String applicationId) {
		log.trace("invoked getApplicationConfigForApplication");

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
	@Path(CONFIG_PATH + "/{applicationConfigId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getApplicationConfig(@PathParam("applicationConfigId") String applicationConfigId) {
		log.trace("getApplicationConfig with configId={}", applicationConfigId);

		ApplicationConfig config = applicationConfigDao.getApplicationConfig(applicationConfigId);
		if (config == null) {
			log.warn("Could not find ApplicationConfig with id={}", applicationConfigId);
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
	@Path(CONFIG_PATH + "/{applicationConfigId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteApplicationConfig(@PathParam("applicationConfigId") String applicationConfigId) {
		log.debug("deleteApplicationConfig with applicationConfigId={}", applicationConfigId);

		if(applicationConfigDao.canDeleteApplicationConfig(applicationConfigId)){
			ApplicationConfig config = applicationConfigDao.deleteApplicationConfig(applicationConfigId);
			if (config == null) {
				log.warn("Could not find and therefore not delete ApplicationConfig with id={}", applicationConfigId);
				return Response.status(Response.Status.NOT_FOUND).build();
			}
		} else {
			return Response.status(Response.Status.NOT_ACCEPTABLE).build();
		}
		return Response.status(Response.Status.NO_CONTENT).build();
	}

	//TODO Review, does not look good...
	@DELETE
	@Path("/{applicationId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteApplication(@PathParam("applicationId") String applicationId) {
		log.debug("delete application ={}", applicationId);

		if (applicationConfigDao.canDeleteApplication(applicationId)){
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
