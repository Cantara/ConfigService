package no.cantara.cs.persistence;

import no.cantara.cs.dto.ClientEnvironment;
import no.cantara.cs.dto.ClientHeartbeatData;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.Client;
import no.cantara.cs.dto.Config;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
@Service
public class PersistedConfigRepo implements ConfigDao, ClientDao {
    private static final Logger log = LoggerFactory.getLogger(PersistedConfigRepo.class);
    private final Map<String, Application> idToApplication;
    private final Map<String, Config> configs;
    private final Map<String, Client> clients;
    private final Map<String, ClientHeartbeatData> clientHeartbeatDataMap;
    private final Map<String, ClientEnvironment> clientEnvironmentMap;
    private final Map<String, String> applicationIdToConfigIdMapping;

    private DB db;

    @Autowired
    public PersistedConfigRepo(
            @Value("${mapdb.path}") String mapDbPath) {
        File mapDbPathFile = new File(mapDbPath);
        log.debug("Using MapDB from {}", mapDbPathFile.getAbsolutePath());
        mapDbPathFile.getParentFile().mkdirs();
    	db = DBMaker.newFileDB(mapDbPathFile).make();
    	
        this.idToApplication = db.getHashMap("idToApplication");
        this.configs = db.getHashMap("configs");
        this.clients = db.getHashMap("clients");
        this.applicationIdToConfigIdMapping = db.getHashMap("applicationIdToConfigIdMapping");
        this.clientHeartbeatDataMap = db.getHashMap("clientHeartbeatData");
        this.clientEnvironmentMap = db.getHashMap("clientEnvironment");
    }

    @Override
    public Application createApplication(Application newApplication) {
        if (idToApplication.values().stream().anyMatch(a -> newApplication.artifactId.equals(a.artifactId))) {
            log.warn("CreateApplication with same artifactId already exists, artifactId: {}", newApplication.artifactId);
            throw new IllegalArgumentException("Application with same artifactId already exists, artifactId: " + newApplication.artifactId);
        }
        newApplication.id = UUID.randomUUID().toString();
        idToApplication.put(newApplication.id, newApplication);
        log.info("created {}", newApplication);
        db.commit();
        return newApplication;
    }

    @Override
    public Config createConfig(String applicationId, Config newConfig) {
        newConfig.setId(UUID.randomUUID().toString());
        configs.put(newConfig.getId(), newConfig);
        applicationIdToConfigIdMapping.put(applicationId, newConfig.getId());
        db.commit();
        return newConfig;
    }

    @Override
    public Config getConfig(String configId) {
        return configs.get(configId);
    }

    @Override
    public Config deleteConfig(String configId) {
    	Config config = configs.remove(configId);
    	db.commit();
        return config;
    }

    @Override
    public Config findConfigByArtifactId(String artifactId) {
        Application application = findApplication(artifactId);
        if (application == null) {
            return null;
        }

        String configId = applicationIdToConfigIdMapping.get(application.id);
        if (configId == null) {
            return null;
        }
        return configs.get(configId);
    }

    private Application findApplication(String artifactId) {
        for (Map.Entry<String, Application> entry : idToApplication.entrySet()) {
            if (entry.getValue().artifactId.equals(artifactId)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public Client getClient(String clientId) {
        return clients.get(clientId);
    }

    @Override
    public void saveClient(Client client) {
        if (client.applicationConfigId == null) {
            throw new IllegalArgumentException("client.applicationConfigId is required");
        }
        // Verify applicationConfig exists
        Config config = getConfig(client.applicationConfigId);
        if (config == null) {
            throw new IllegalArgumentException("No ApplicationConfig was found with id: " + client.applicationConfigId);
        }
        clients.put(client.clientId, client);
        db.commit();
    }

    @Override
    public Config updateConfig(Config updatedConfig) {
    	Config config = configs.put(updatedConfig.getId(), updatedConfig);
    	db.commit();
        return config;
    }

    @Override
    public String getArtifactId(Config config) {
        String configId = config.getId();
        String applicationId = applicationIdToConfigIdMapping.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(configId))
                .findFirst()
                .get()
                .getKey();

        Application application = idToApplication.get(applicationId);
        return application.artifactId;
    }

    @Override
    public Map<String, Config> getAllConfigs() {
        return configs;
    }

    @Override
    public List<Application> getApplications() {
        return new ArrayList<>(idToApplication.values());
    }

    @Override
    public ClientHeartbeatData getClientHeartbeatData(String clientId) {
        return clientHeartbeatDataMap.get(clientId);
    }

    @Override
    public void saveClientHeartbeatData(String clientId, ClientHeartbeatData clientHeartbeatData) {
        clientHeartbeatDataMap.put(clientId, clientHeartbeatData);
    }

    @Override
    public void saveClientEnvironment(String clientId, ClientEnvironment clientEnvironment) {
        clientEnvironmentMap.put(clientId, clientEnvironment);
    }

    @Override
    public ClientEnvironment getClientEnvironment(String clientId) {
        return clientEnvironmentMap.get(clientId);
    }

    @Override
    public Map<String, ClientHeartbeatData> getAllClientHeartbeatData(String artifactId) {
        return clientHeartbeatDataMap.entrySet().stream()
                .filter(e -> e.getValue().artifactId.equals(artifactId))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
