package no.cantara.jau.persistence;

import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistration;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-08-23.
 */
@Service
public class ConfigSearcher {
    private static final Logger log = LoggerFactory.getLogger(ConfigSearcher.class);
    private final ServiceConfigDao dao;

    @Autowired
    public ConfigSearcher(ServiceConfigDao dao) {
        this.dao = dao;
    }

    /**
     * @return null if request is valid, but no ServiceConfig can be found or a new ClientConfig containing the ServiceConfig and a ClientId.
     * @throws IllegalArgumentException if request does not contain enough information
     */
    public ClientConfig registerClient(ClientRegistration registration) {
        ServiceConfig serviceConfig = dao.findByArtifactId(registration.artifactId);
        if (serviceConfig == null) {
            log.warn("No ServiceConfig was found for artifactId={}", registration.artifactId);
            return null;
        }
        ClientConfig clientConfig = new ClientConfig(UUID.randomUUID().toString(), serviceConfig);

        //TODO persist registration.envInfo
        //TODO persist coupling between clientConfig.clientId and serviceConfig
        return clientConfig;
    }
}
