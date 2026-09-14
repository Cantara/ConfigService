package no.cantara.cs.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;

import no.cantara.cs.Main;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationStatus;
import no.cantara.cs.dto.ClientEnvironment;
import no.cantara.cs.dto.ClientHeartbeatData;
import no.cantara.cs.persistence.ApplicationConfigDao;
import no.cantara.cs.persistence.ClientDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-12.
 */
@Path(ApplicationResource.APPLICATION_PATH)
public class ApplicationResource {
    public static final String APPLICATION_PATH = "/application";
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ApplicationConfigDao applicationConfigDao;
    private final ClientDao clientDao;



    @Autowired
    public ApplicationResource(ApplicationConfigDao applicationConfigDao, ClientDao clientDao) {
        this.applicationConfigDao = applicationConfigDao;
        this.clientDao = clientDao;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApplication(String json) {
        log.trace("Invoked createApplication with {}", json);
        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
        String artifactId =  JsonPath.read(document, "$.artifactId");
        if (artifactId == null) {
            Response.Status status = Response.Status.BAD_REQUEST;
            log.warn("Invalid json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
            return Response.status(status).build();
        }

        Application application;
        try {
            application = applicationConfigDao.createApplication(new Application(artifactId));
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }


        String createdJson;
        try {
            createdJson = mapper.writeValueAsString(application);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", application.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(createdJson).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllApplications() {
        log.trace("getAllApplications");
        List<Application> applications = applicationConfigDao.getApplications();
        String jsonResponse;
        try {
            jsonResponse = mapper.writeValueAsString(applications);
        } catch (JsonProcessingException e) {
            log.warn("Could not convert to Json {}", applications);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResponse).build();
    }
    
    @GET
    @Path("/{artifactId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicationByArtifact(@PathParam("artifactId") String artifactId) {
        log.trace("getApplicationByArtifactId");
        Application app = applicationConfigDao.getApplication(artifactId);
        String jsonResponse;
        try {
            jsonResponse = mapper.writeValueAsString(app);
        } catch (JsonProcessingException e) {
            log.warn("Could not convert to Json {}", app);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(jsonResponse).build();
    }
    
    @GET
    @Path("/{artifactId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicationStatus(@PathParam("artifactId") String artifactId) {
        log.trace("getApplicationStatus, artifactId={}", artifactId);

        Map<String, ClientHeartbeatData> allClientHeartbeatData = clientDao.getAllClientHeartbeatData(artifactId);

        ApplicationStatus applicationStatus = new ApplicationStatus(allClientHeartbeatData);
        for(String key : allClientHeartbeatData.keySet()) {
        	if(Arrays.asList(ClientAdminResource.defaultClientNameList).contains(allClientHeartbeatData.get(key).clientName)) {
        		ClientEnvironment ce = clientDao.getClientEnvironment(key);
        		allClientHeartbeatData.get(key).clientName = ClientAdminResource.makeUpADefaultClientName(ce);
			}
        }
      
		
        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(applicationStatus);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", allClientHeartbeatData.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(jsonResult).build();
    }
}
