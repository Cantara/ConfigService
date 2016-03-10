package no.cantara.cs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import no.cantara.cs.client.ClientStatus;
import no.cantara.cs.persistence.StatusDao;
import no.cantara.cs.persistence.ConfigDao;
import no.cantara.cs.dto.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-12.
 */
@Path(ApplicationResource.APPLICATION_PATH)
public class ApplicationResource {
    public static final String APPLICATION_PATH = "/application";
    private static final Logger log = LoggerFactory.getLogger(ApplicationResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ConfigDao configDao;
    private final StatusDao statusDao;

    @Autowired
    public ApplicationResource(ConfigDao configDao, StatusDao statusDao) {
        this.configDao = configDao;
        this.statusDao = statusDao;
    }

    @POST
    @Path("/")
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
        Application application = configDao.createApplication(new Application(artifactId));
        log.info("created {}", application);

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
    @Path("/{artifactId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusForArtifactInstances(@PathParam("artifactId") String artifactId) {
        log.trace("getStatusForArtifactInstances, artifactId={}", artifactId);

        Map<String, ClientStatus> allStatuses = statusDao.getAllStatuses(artifactId);
        Map<String, Object> response = new HashMap<>();
        Instant oneHourAgo = new Date().toInstant().minusSeconds(60 * 60);

        List<String> seenInTheLastHour = allStatuses.entrySet().stream()
                .filter(e -> Instant.parse(e.getValue().timeOfContact).isAfter(oneHourAgo))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        long seenInTheLastHourCount = seenInTheLastHour.size();

        String jsonResult;
        try {
            response.put("numberOfRegisteredClients", allStatuses.size());
            response.put("seenInTheLastHourCount", seenInTheLastHourCount);
            response.put("seenInTheLastHour", seenInTheLastHour);
            response.put("allClientsSnapshot", allStatuses);
            jsonResult = mapper.writeValueAsString(response);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", allStatuses.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(jsonResult).build();
    }
}
