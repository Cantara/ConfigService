package no.cantara.jau.persistence;

import no.cantara.jau.serviceconfig.dto.Application;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;

import javax.xml.ws.Service;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public interface ServiceConfigDao {
    Application createApplication(Application newApplication);


    void addOrUpdateConfig(String clientId, ServiceConfig serviceConfig);
    ServiceConfig findConfig(String clientId);

    ServiceConfig createServiceConfig(String applicationId, ServiceConfig newServiceConfig);

    ServiceConfig findByArtifactId(String artifactId);

    void registerClient(String clientId, String serviceConfigId);
    ServiceConfig findByClientId(String clientId);

    ServiceConfig getServiceConfig(String serviceConfigId);

    ServiceConfig deleteServiceConfig(String serviceConfigId);

    ServiceConfig updateServiceConfig(ServiceConfig updatedServiceConfig);

    String getArtifactId(ServiceConfig serviceConfig);

    ServiceConfig changeServiceConfigForClientToUse(String clientId, String serviceConfigId);
}
