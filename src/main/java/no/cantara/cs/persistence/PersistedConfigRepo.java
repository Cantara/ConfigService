package no.cantara.cs.persistence;

import no.cantara.cs.client.ClientAlreadyRegisteredException;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.Config;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
@Service
public class PersistedConfigRepo implements ConfigDao {
    private static final Logger log = LoggerFactory.getLogger(PersistedConfigRepo.class);
    private final Map<String, Application> idToApplication;
    private final Map<String, Config> configs;
    private final Map<String, String> applicationIdToConfigIdMapping;
    private final Map<String, String> clientIdToConfigIdMapping;
    private final Set<String> registeredClientIds;

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
        this.applicationIdToConfigIdMapping = db.getHashMap("applicationIdToConfigIdMapping");
        this.clientIdToConfigIdMapping = db.getHashMap("clientIdToConfigIdMapping");
        this.registeredClientIds = db.getHashSet("registeredClientIds");
    }

    @Override
    public Application createApplication(Application newApplication) {
        newApplication.id = UUID.randomUUID().toString();
        idToApplication.put(newApplication.id, newApplication);
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
    public Config findByArtifactId(String artifactId) {
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
    public void registerClient(String clientId, String configId) {
        if (registeredClientIds.contains(clientId)) {
            throw new ClientAlreadyRegisteredException(clientId);
        }
        registeredClientIds.add(clientId);
        clientIdToConfigIdMapping.put(clientId, configId);
        db.commit();
    }

    @Override
    public Config findByClientId(String clientId) {
        String configId = clientIdToConfigIdMapping.get(clientId);
        if (configId == null) {
            return null;
        }
        return configs.get(configId);
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

    public void addOrUpdateConfig(String applicationId, Config config) {
        String configId = config.getId();
        if (configId == null) {
            Config persistedConfig = createConfig(applicationId, config);
            configId = persistedConfig.getId();
        } else {
            updateConfig(config);
        }

        applicationIdToConfigIdMapping.put(applicationId, configId);
        db.commit();
    }

    //Should probably be moved to somewhere else.
    @Deprecated
    public Config findConfig(String clientId) {
        String configId = applicationIdToConfigIdMapping.get(clientId);
        if (configId == null) {
            return null;
        }
        return configs.get(configId);
    }

    @Override
    public Config changeConfigForClientToUse(String clientId, String configId) {
        Config config = configs.get(configId);
        if (config != null) {
            clientIdToConfigIdMapping.put(clientId, configId);
            db.commit();
        }
        return config;
    }

    @Override
    public Map<String, Config> getAllConfigs() {
        return configs;
    }

    /*
    private void addTestData() {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.1-SNAPSHOT");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "snapshots").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);

        Config Config = new ServiceConfig("Service1-1.23");
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());
        addOrUpdateConfig("UserAdminService", serviceConfig);
    }
    */
}
