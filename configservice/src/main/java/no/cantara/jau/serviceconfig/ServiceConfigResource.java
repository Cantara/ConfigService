package no.cantara.jau.serviceconfig;

import no.cantara.jau.persistence.ServiceConfigDao;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
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

    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createServiceConfig(String json) {
        log.trace("createServiceConfig");

        ServiceConfig newServiceConfig;
        try {
            newServiceConfig = ServiceConfigSerializer.fromJson(json);
        } catch (RuntimeException e) {
            Response.Status status = Response.Status.BAD_REQUEST;
            log.warn("Could not parse json. Returning {} {}, json={}", status.getStatusCode(), status.getReasonPhrase(), json);
            return Response.status(status).build();
        }

        try {
            ServiceConfig persistedServiceConfig = serviceConfigDao.create(newServiceConfig);
            String jsonResult = ServiceConfigSerializer.toJson(persistedServiceConfig);
            return Response.ok(jsonResult).build(); // Shouldn't this be 201 Created? http://www.restapitutorial.com/lessons/httpmethods.html
        } catch (RuntimeException e) {
            log.error("", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/{serviceConfigId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getServiceConfig(@PathParam("serviceConfigId") String serviceConfigId) {
        log.trace("getServiceConfig with serviceConfigId={}", serviceConfigId);

        ServiceConfig serviceConfig = serviceConfigDao.get(serviceConfigId);
        if (serviceConfig != null) {
            String jsonResult = ServiceConfigSerializer.toJson(serviceConfig);
            return Response.ok(jsonResult).build();
        } else {
            log.warn("Could not find serviceConfig with id={}", serviceConfigId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("/{serviceConfigId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteServiceConfig(@PathParam("serviceConfigId") String serviceConfigId) {
        log.trace("deleteServiceConfig with serviceConfigId={}", serviceConfigId);

        ServiceConfig serviceConfig = serviceConfigDao.delete(serviceConfigId);
        if (serviceConfig != null) {
            return Response.status(Response.Status.NO_CONTENT).build();
        } else {
            log.warn("Could not find and therefore not delete serviceConfig with id={}", serviceConfigId);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }


    //http://localhost:8086/jau/serviceconfig/query?clientid=clientid1
    /**
     * @param clientid  unique id of client which query for config
     * @return  a representation of ServiceConfig for this client
     */
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated //use /clientconfig/{clientId} instead 
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
