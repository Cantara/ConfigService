package no.cantara.jau.persistence;

import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-08-23.
 */
public class ConfigSearcher {
    private final ServiceConfigDao dao;

    @Autowired
    public ConfigSearcher(ServiceConfigDao dao) {
        this.dao = dao;
    }

    /**
     * @return null if request is valid, but no ServiceConfig can be found or a new ClientConfig containing the ServiceConfig and a ClientId.
     * @throws IllegalArgumentException if request does not contain enough information
     */
    public ClientConfig registerClient(String json) {
        String artifactId = "UserAdminService"; //extract from json

        ServiceConfig serviceConfig = dao.findByArtifactId(artifactId);
        if (serviceConfig == null) {
            return null;
        }
        return new ClientConfig(UUID.randomUUID().toString(), serviceConfig);
    }
}
