package no.cantara.jau.serviceconfig;

import no.cantara.jau.serviceconfig.dto.ServiceConfig;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public interface ServiceConfigDao {
    void addOrUpdateConfig(String clientId, ServiceConfig serviceConfig);
    ServiceConfig findConfig(String clientId);

    ServiceConfig create(ServiceConfig newServiceConfig);
}
