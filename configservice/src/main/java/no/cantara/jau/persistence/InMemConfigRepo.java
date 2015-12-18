package no.cantara.jau.persistence;

import no.cantara.jau.serviceconfig.dto.Application;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * This class is a mess. Should be totally redesigned after the public API is stable.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
//Add @Service and remove @Service from PersistedConfigRepo to activate.
public class InMemConfigRepo implements ServiceConfigDao {
    private final Map<String, Application> idToApplication;
    private final Map<String, ServiceConfig> serviceConfigs;
    private final Map<String, String> applicationIdToServiceConfigIdMapping;
    private final Map<String, String> clientIdToServiceConfigIdMapping;


    public InMemConfigRepo() {
        this.idToApplication = new HashMap<>();
        this.serviceConfigs = new HashMap<>();
        this.applicationIdToServiceConfigIdMapping = new HashMap<>();
        this.clientIdToServiceConfigIdMapping = new HashMap<>();
        //addTestData();
    }

    @Override
    public Application createApplication(Application newApplication) {
        newApplication.id = UUID.randomUUID().toString();
        idToApplication.put(newApplication.id, newApplication);
        return newApplication;
    }

    @Override
    public ServiceConfig createServiceConfig(String applicationId, ServiceConfig newServiceConfig) {
        newServiceConfig.setId(UUID.randomUUID().toString());
        serviceConfigs.put(newServiceConfig.getId(), newServiceConfig);
        applicationIdToServiceConfigIdMapping.put(applicationId, newServiceConfig.getId());
        return newServiceConfig;
    }

    @Override
    public ServiceConfig getServiceConfig(String serviceConfigId) {
        return serviceConfigs.get(serviceConfigId);
    }

    @Override
    public ServiceConfig deleteServiceConfig(String serviceConfigId) {
        return serviceConfigs.remove(serviceConfigId);
    }

    @Override
    public ServiceConfig findByArtifactId(String artifactId) {
        Application application = findApplication(artifactId);
        if (application == null) {
            return null;
        }

        String serviceConfigId = applicationIdToServiceConfigIdMapping.get(application.id);
        if (serviceConfigId == null) {
            return null;
        }
        return serviceConfigs.get(serviceConfigId);
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
    public void registerClient(String clientId, String serviceConfigId) {
        clientIdToServiceConfigIdMapping.put(clientId, serviceConfigId);
    }

    @Override
    public ServiceConfig findByClientId(String clientId) {
        String serviceConfigId = clientIdToServiceConfigIdMapping.get(clientId);
        if (serviceConfigId == null) {
            return null;
        }
        return serviceConfigs.get(serviceConfigId);
    }

    @Override
    public ServiceConfig updateServiceConfig(ServiceConfig newServiceConfig) {
        return serviceConfigs.put(newServiceConfig.getId(), newServiceConfig);
    }

    @Override
    public String getArtifactId(ServiceConfig serviceConfig) {
        String serviceConfigId = serviceConfig.getId();
        String applicationId = applicationIdToServiceConfigIdMapping.entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(serviceConfigId))
                .findFirst()
                .get()
                .getKey();

        Application application = idToApplication.get(applicationId);
        return application.artifactId;
    }

    @Override
    public ServiceConfig changeServiceConfigForClientToUse(String clientId, String serviceConfigId) {
        //TODO: Implementation
        return null;
    }

    @Override
    public Map<String, ServiceConfig> getAllServiceConfigs() {
        //TODO: Implementation
        return null;
    }

    public void addOrUpdateConfig(String applicationId, ServiceConfig serviceConfig) {
        String serviceConfigId = serviceConfig.getId();
        if (serviceConfigId == null) {
            ServiceConfig persistedServiceConfig = createServiceConfig(applicationId, serviceConfig);
            serviceConfigId = persistedServiceConfig.getId();
        } else {
            updateServiceConfig(serviceConfig);
        }

        applicationIdToServiceConfigIdMapping.put(applicationId, serviceConfigId);
    }

    //Should probably be moved to somewhere else.
    @Deprecated
    public ServiceConfig findConfig(String clientId) {
        String serviceConfigId = applicationIdToServiceConfigIdMapping.get(clientId);
        if (serviceConfigId == null) {
            return null;
        }
        return serviceConfigs.get(serviceConfigId);
    }


    /*
    private void addTestData() {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.1-SNAPSHOT");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "snapshots").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);

        ServiceConfig serviceConfig = new ServiceConfig("Service1-1.23");
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());
        addOrUpdateConfig("UserAdminService", serviceConfig);
    }
    */
}
