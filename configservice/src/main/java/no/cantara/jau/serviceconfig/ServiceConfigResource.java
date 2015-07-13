package no.cantara.jau.serviceconfig;

import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * CRUD, http endpoint for ServiceConfig
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a>
 */
@Path("/serviceconfig")
public class ServiceConfigResource {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigResource.class);
    private final ServiceConfigDao serviceConfigDao;

    @Autowired
    public ServiceConfigResource(ServiceConfigDao serviceConfigDao) {
        this.serviceConfigDao = serviceConfigDao;
    }


    /**
     * @param clientid  unique id of client which query for config
     * @return  a representation of ServiceConfig for this client
     */
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findServiceConfig(@QueryParam("clientid") String clientid) {
        log.trace("findServiceConfig with clientid={}", clientid);
        try {
            ServiceConfig serviceConfig = serviceConfigDao.findConfig(clientid);
            if (serviceConfig == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String json = ServiceConfigSerializer.toJson(serviceConfig);
            log.trace("findServiceConfig with clientid={} returned {}", clientid, serviceConfig);
            return Response.ok(json).build();
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
