package no.cantara.cs.client;

import no.cantara.cs.persistence.ConfigDao;
import no.cantara.cs.persistence.EventsDao;
import no.cantara.cs.persistence.StatusDao;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.dto.Config;
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
    private final ConfigDao dao;
    private final StatusDao statusDao;
    private final EventsDao eventsDao;

    @Autowired
    public ClientService(ConfigDao dao, StatusDao statusDao, EventsDao eventsDao) {
        this.dao = dao;
        this.statusDao = statusDao;
        this.eventsDao = eventsDao;
    }

    /**
     * @return null if request is valid, but no ServiceConfig can be found or a new ClientConfig containing the ServiceConfig and a ClientId.
     * @throws IllegalArgumentException if request does not contain enough information
     */
    public ClientConfig registerClient(ClientRegistrationRequest registration) {
        Config config = dao.findByArtifactId(registration.artifactId);
        if (config == null) {
            log.warn("No ServiceConfig was found for artifactId={}", registration.artifactId);
            return null;
        }
        ClientConfig clientConfig = new ClientConfig(UUID.randomUUID().toString(), config);
        dao.registerClient(clientConfig.clientId, clientConfig.config.getId());

        statusDao.saveStatus(clientConfig.clientId, new ClientStatus(registration));

        return clientConfig;
    }

    public ClientConfig checkForUpdatedClientConfig(String clientId, CheckForUpdateRequest checkForUpdateRequest) {
        Config config = dao.findByClientId(clientId);
        if (config == null) {
            log.warn("No ServiceConfig was found for clientId={}", clientId);
            return null;
        }
        String artifactId = dao.getArtifactId(config);
        statusDao.saveStatus(clientId, new ClientStatus(checkForUpdateRequest, artifactId));
        eventsDao.saveEvents(clientId, checkForUpdateRequest.eventsStore);
        return new ClientConfig(clientId, config);
    }
}
