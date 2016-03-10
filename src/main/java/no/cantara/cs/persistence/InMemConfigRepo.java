package no.cantara.cs.persistence;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is a mess. Should be totally redesigned after the public API is stable.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
//Add @Service and remove @Service from PersistedConfigRepo to activate.
public class InMemConfigRepo implements ConfigDao {
    private final Map<String, Application> idToApplication;
    private final Map<String, Config> configs;
    private final Map<String, String> applicationIdToConfigIdMapping;
    private final Map<String, String> clientIdToConfigIdMapping;


    public InMemConfigRepo() {
        this.idToApplication = new HashMap<>();
        this.configs = new HashMap<>();
        this.applicationIdToConfigIdMapping = new HashMap<>();
        this.clientIdToConfigIdMapping = new HashMap<>();
        //addTestData();
    }

    @Override
    public Application createApplication(Application newApplication) {
        newApplication.id = UUID.randomUUID().toString();
        idToApplication.put(newApplication.id, newApplication);
        return newApplication;
    }

    @Override
    public Config createConfig(String applicationId, Config newConfig) {
        newConfig.setId(UUID.randomUUID().toString());
        configs.put(newConfig.getId(), newConfig);
        applicationIdToConfigIdMapping.put(applicationId, newConfig.getId());
        return newConfig;
    }

    @Override
    public Config getConfig(String configId) {
        return configs.get(configId);
    }

    @Override
    public Config deleteConfig(String configId) {
        return configs.remove(configId);
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
        clientIdToConfigIdMapping.put(clientId, configId);
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
        return configs.put(updatedConfig.getId(), updatedConfig);
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
    public Config changeConfigForClientToUse(String clientId, String configId) {
        //TODO: Implementation
        return null;
    }

    @Override
    public Map<String, Config> getAllConfigs() {
        //TODO: Implementation
        return null;
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
