package no.cantara.jau.clientconfig;

import no.cantara.jau.persistence.EventsDao;
import no.cantara.jau.persistence.StatusDao;
import no.cantara.jau.persistence.ServiceConfigDao;
import no.cantara.jau.serviceconfig.dto.CheckForUpdateRequest;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
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
public class ClientService {
    private static final Logger log = LoggerFactory.getLogger(ClientService.class);
    private final ServiceConfigDao dao;
    private final StatusDao statusDao;
    private final EventsDao eventsDao;

    @Autowired
    public ClientService(ServiceConfigDao dao, StatusDao statusDao, EventsDao eventsDao) {
        this.dao = dao;
        this.statusDao = statusDao;
        this.eventsDao = eventsDao;
    }

    /**
     * @return null if request is valid, but no ServiceConfig can be found or a new ClientConfig containing the ServiceConfig and a ClientId.
     * @throws IllegalArgumentException if request does not contain enough information
     */
    public ClientConfig registerClient(ClientRegistrationRequest registration) {
        ServiceConfig serviceConfig = dao.findByArtifactId(registration.artifactId);
        if (serviceConfig == null) {
            log.warn("No ServiceConfig was found for artifactId={}", registration.artifactId);
            return null;
        }
        ClientConfig clientConfig = new ClientConfig(UUID.randomUUID().toString(), serviceConfig);
        dao.registerClient(clientConfig.clientId, clientConfig.serviceConfig.getId());

        statusDao.saveStatus(clientConfig.clientId, new ClientStatus(registration));

        return clientConfig;
    }

    public ClientConfig checkForUpdatedClientConfig(String clientId, CheckForUpdateRequest checkForUpdateRequest) {
        ServiceConfig serviceConfig = dao.findByClientId(clientId);
        if (serviceConfig == null) {
            log.warn("No ServiceConfig was found for clientId={}", clientId);
            return null;
        }
        String artifactId = dao.getArtifactId(serviceConfig);
        statusDao.saveStatus(clientId, new ClientStatus(checkForUpdateRequest, artifactId));
        eventsDao.saveEvents(clientId, checkForUpdateRequest.eventsStore);
        return new ClientConfig(clientId, serviceConfig);
    }
}
