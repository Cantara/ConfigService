package no.cantara.jau.serviceconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import no.cantara.jau.persistence.ServiceConfigDao;
import no.cantara.jau.serviceconfig.dto.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-12.
 */
@Path(ApplicationResource.APPLICATION_PATH)
public class ApplicationResource {
    public static final String APPLICATION_PATH = "/application";
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigResource.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ServiceConfigDao serviceConfigDao;

    @Autowired
    public ApplicationResource(ServiceConfigDao serviceConfigDao) {
        this.serviceConfigDao = serviceConfigDao;
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
        Application application = serviceConfigDao.createApplication(new Application(artifactId));
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
}
