package no.cantara.cs.client;

import no.cantara.cs.dto.*;
import no.cantara.cs.persistence.ClientDao;
import no.cantara.cs.persistence.ConfigDao;
import no.cantara.cs.persistence.EventsDao;
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
    private final EventsDao eventsDao;
    private final ClientDao clientDao;

    @Autowired
    public ClientService(ConfigDao dao, EventsDao eventsDao, ClientDao clientDao) {
        this.dao = dao;
        this.eventsDao = eventsDao;
        this.clientDao = clientDao;
    }

    /**
     * @return null if request is valid, but no Config can be found or a new ClientConfig containing the Config and a ClientId.
     * @throws IllegalArgumentException if request does not contain enough information
     */
    public ClientConfig registerClient(ClientRegistrationRequest registration) {

        Client client;
        Config config;

        if (registration.clientId != null) {
            client = clientDao.getClient(registration.clientId);
            if (client == null) {
                log.warn("RegisterClient called with a non-existing clientId: {}", registration.clientId);
                throw new IllegalArgumentException("No client was found with clientId: " + registration.clientId);
            }
            if (clientDao.getClientHeartbeatData(client.clientId) != null) {
                log.warn("RegisterClient called with already registered clientId: {}", registration.clientId);
                throw new IllegalArgumentException("Client is already registered, clientId: " + registration.clientId);
            }
            config = dao.getConfig(client.applicationConfigId);
            if (config == null) {
                log.warn("No Config was found for clientId={}", registration.clientId);
                return null;
            }
        } else {
            config = dao.findConfigByArtifactId(registration.artifactId);
            if (config == null) {
                log.warn("No Config was found for artifactId={}", registration.artifactId);
                return null;
            }
            client = new Client(UUID.randomUUID().toString(), config.getId(), true);
        }

        clientDao.saveClient(client);
        clientDao.saveClientHeartbeatData(client.clientId, new ClientHeartbeatData(registration, config));
        clientDao.saveClientEnvironment(client.clientId, new ClientEnvironment(registration.envInfo));
        return new ClientConfig(client.clientId, config) ;
    }

    public ClientConfig checkForUpdatedClientConfig(String clientId, CheckForUpdateRequest checkForUpdateRequest) {
        Config config = findConfigByClientId(clientId);
        if (config == null) {
            log.warn("No ApplicationConfig was found for clientId={}", clientId);
            return null;
        }
        String artifactId = dao.getArtifactId(config);
        clientDao.saveClientHeartbeatData(clientId, new ClientHeartbeatData(checkForUpdateRequest, config, artifactId));
        clientDao.saveClientEnvironment(clientId, new ClientEnvironment(checkForUpdateRequest.envInfo));

        eventsDao.saveEvents(clientId, checkForUpdateRequest.eventsStore);

        ClientConfig clientConfig = new ClientConfig(clientId, null);

        if (!config.getLastChanged().equals(checkForUpdateRequest.configLastChanged)) {
            // Return clientConfig.config only in case the config was changed
            clientConfig.config = config;
        }
        return clientConfig;
    }

    public Config findConfigByClientId(String clientId) {
        Client client = clientDao.getClient(clientId);
        if (client == null) {
            return null;
        }
        Config config = dao.getConfig(client.applicationConfigId);
        if (config == null) {
            return null;
        }
        return config;
    }

}
