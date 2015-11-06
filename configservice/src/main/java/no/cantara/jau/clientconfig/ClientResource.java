package no.cantara.jau.clientconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.persistence.StatusDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    private final StatusDao dao;

    @Autowired
    public ClientResource(StatusDao dao) {
        this.dao = dao;
    }

    @GET
    @Path("/{clientId}/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusForAll(@PathParam("clientId") String clientId) {
        log.trace("getStatusForAll");
        ClientStatus status = dao.getStatus(clientId);

        String jsonResult;
        try {
            jsonResult = mapper.writeValueAsString(status);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", status.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(jsonResult).build();
    }

}
