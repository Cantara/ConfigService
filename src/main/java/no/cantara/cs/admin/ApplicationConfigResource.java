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
import java.util.List;
import java.util.Map;

/**
 * CRUD, http endpoint for ApplicationConfig
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path(ApplicationResource.APPLICATION_PATH)
public class ApplicationConfigResource {
	public static final String CONFIG_PATH_WITH_APPID = "/{applicationId}/config";
	public static final String CONFIG_PATH = "/config";

	private static final Logger log = LoggerFactory.getLogger(ApplicationConfigResource.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	private final ApplicationConfigDao applicationConfigDao;

	@Autowired
	public ApplicationConfigResource(ApplicationConfigDao applicationConfigDao) {
		this.applicationConfigDao = applicationConfigDao;
	}

	//associate an appId to a configId
	@POST
	@Path(CONFIG_PATH_WITH_APPID)
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response LinkAppToApplicationConfig(@PathParam("applicationId") String applicationId, String json) {
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

	//update a config
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
	
	//provide compatibility for older config-service SDK version
	@PUT
	@Path(CONFIG_PATH_WITH_APPID + "/{applicationConfigId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateApplicationConfig2(@PathParam("applicationId") String applicationId, @PathParam("applicationConfigId") String applicationConfigId, String json) {
		log.debug("Invoked updateApplicationConfig with json {}", json);
		return updateApplicationConfig(applicationConfigId, json);
	}

	//list all configs
	@GET
	@Path(CONFIG_PATH)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllApplicationConfigs() {
		log.trace("invoked getAllApplicationConfigs");

		List<ApplicationConfig> allConfigs = applicationConfigDao.getAllConfigs();

		String jsonResult;
		try {
			jsonResult = mapper.writeValueAsString(allConfigs);
		} catch (JsonProcessingException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		return Response.ok(jsonResult).build();
	}
	
	//get the latest config for this app
	@GET
	@Path(CONFIG_PATH_WITH_APPID)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getApplicationConfigForApplication(@PathParam("applicationId") String applicationId) {
		log.trace("invoked getApplicationConfigForApplication");

		ApplicationConfig applicationConfig = applicationConfigDao.findTheLatestApplicationConfigByApplicationId(applicationId);
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
	
	//get all configs for this app
	@GET
	@Path(CONFIG_PATH_WITH_APPID + "/list")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllApplicationConfigsForApplication(@PathParam("applicationId") String applicationId) {
		log.trace("invoked getAllApplicationConfigsForApplication");

		List<ApplicationConfig> applicationConfigList = applicationConfigDao.findAllApplicationConfigsByApplicationId(applicationId);
		if (applicationConfigList == null) {
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		String jsonResult;
		try {
			jsonResult = mapper.writeValueAsString(applicationConfigList);
		} catch (JsonProcessingException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		return Response.ok(jsonResult).build();
	}

	//get a config by config id
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
	
	//TODO: will remove this use in SDK
	//for compatibility with older SDK version
	@GET
	@Path(CONFIG_PATH_WITH_APPID + "/{applicationConfigId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getApplicationConfig2(@PathParam("applicationId") String applicationId, @PathParam("applicationConfigId") String applicationConfigId) {
		log.trace("getApplicationConfig with configId={}", applicationConfigId);
		return getApplicationConfig(applicationConfigId);
	}
	
	@GET
	@Path(CONFIG_PATH + "/findartifactid/{applicationConfigId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getArtifactIdForThisAppConfig(@PathParam("applicationConfigId") String applicationConfigId) {
		log.trace("getArtifactIdForThisAppConfig with configId={}", applicationConfigId);

		
		ApplicationConfig config = applicationConfigDao.getApplicationConfig(applicationConfigId);
		if (config == null) {
			log.warn("Could not find ApplicationConfig with id={}", applicationConfigId);
			return Response.status(Response.Status.NOT_FOUND).build();
		}
		String artifactId = applicationConfigDao.getArtifactId(config);

		String jsonResult;
		try {
			jsonResult = mapper.writeValueAsString(artifactId);
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
	
	@DELETE
	@Path(CONFIG_PATH_WITH_APPID + "/{applicationConfigId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteApplicationConfig2(@PathParam("applicationId") String applicationId, @PathParam("applicationConfigId") String applicationConfigId) {
		log.debug("deleteApplicationConfig with applicationConfigId={}", applicationConfigId);
		return deleteApplicationConfig(applicationConfigId);
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
	
	@GET
	@Path("/canDeleteApp/{applicationId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response canDeleteThisApp(@PathParam("applicationId") String applicationId) {
		log.debug("canDeleteThisApp ={}", applicationId);
		String jsonResult;
		try {
			jsonResult = mapper.writeValueAsString(applicationConfigDao.canDeleteApplication(applicationId));
		} catch (JsonProcessingException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		return Response.ok(jsonResult).build();
	}
	
	@GET
	@Path("/canDeleteAppConfig/{configId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response canDeleteThisAppConfig(@PathParam("configId") String configId) {
		log.debug("canDeleteThisAppConfig ={}", configId);
		String jsonResult;
		try {
			jsonResult = mapper.writeValueAsString(applicationConfigDao.canDeleteApplicationConfig(configId));
		} catch (JsonProcessingException e) {
			log.error("", e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
		return Response.ok(jsonResult).build();
	}

}
