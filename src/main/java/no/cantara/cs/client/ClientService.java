package no.cantara.cs.client;

import no.cantara.cs.cloudwatch.CloudWatchLogger;
import no.cantara.cs.cloudwatch.CloudWatchMetricsPublisher;
import no.cantara.cs.dto.*;
import no.cantara.cs.dto.event.ExtractedEventsStore;
import no.cantara.cs.persistence.ApplicationConfigDao;
import no.cantara.cs.persistence.ClientDao;
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
    private final ApplicationConfigDao applicationConfigDao;
    private final EventsDao eventsDao;
    private final ClientDao clientDao;
    private final CloudWatchLogger cloudWatchLogger;
    private final CloudWatchMetricsPublisher cloudWatchMetricsPublisher;

    @Autowired
    public ClientService(ApplicationConfigDao applicationConfigDao, EventsDao eventsDao, ClientDao clientDao,
                         CloudWatchLogger cloudWatchLogger, CloudWatchMetricsPublisher cloudWatchMetricsPublisher) {
        this.applicationConfigDao = applicationConfigDao;
        this.eventsDao = eventsDao;
        this.clientDao = clientDao;
        this.cloudWatchLogger = cloudWatchLogger;
        this.cloudWatchMetricsPublisher = cloudWatchMetricsPublisher;
    }

    /**
     * @return null if request is valid, but no Config can be found or a new ClientConfig containing the Config and a ClientId.
     * @throws IllegalArgumentException if request does not contain enough information
     */
    public ClientConfig registerClient(ClientRegistrationRequest registration) {
        Client client;
        ApplicationConfig config;

        if (registration.clientId != null) {
            client = clientDao.getClient(registration.clientId);
            if (client == null) {
                log.warn("RegisterClient called with a non-existing clientId: {}", registration.clientId);
                throw new IllegalArgumentException("No client was found with clientId: " + registration.clientId);
            }

            config = applicationConfigDao.getApplicationConfig(client.applicationConfigId);
            if (config == null) {
                log.warn("No ApplicationConfig was found for clientId={}", registration.clientId);
                return null;
            }
        } else {
            config = applicationConfigDao.findApplicationConfigByArtifactId(registration.artifactId);
            if (config == null) {
                log.warn("No ApplicationConfig was found for artifactId={}", registration.artifactId);
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
        ApplicationConfig config = findApplicationConfigByClientId(clientId);
        if (config == null) {
            log.warn("No ApplicationConfig was found for clientId={}", clientId);
            return null;
        }
        String artifactId = applicationConfigDao.getArtifactId(config);
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

    public ApplicationConfig findApplicationConfigByClientId(String clientId) {
        Client client = clientDao.getClient(clientId);
        if (client == null) {
            return null;
        }
        return applicationConfigDao.getApplicationConfig(client.applicationConfigId);
    }

    public void processEvents(String clientId, ExtractedEventsStore eventsStore) {
        try {
            cloudWatchLogger.log(clientId, eventsStore);
        } catch (Exception e) {
            log.error("Failed to log events in CloudWatch", e);
        }
        try {
            cloudWatchMetricsPublisher.registerHeartbeat(clientId);
        } catch (Exception e) {
            log.error("Failed to register heartbeat in CloudWatch", e);
        }
    }
}
