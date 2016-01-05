package no.cantara.jau.persistence;

import no.cantara.jau.serviceconfig.dto.Application;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.util.Configuration;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
@Service
public class PersistedConfigRepo implements ServiceConfigDao {
    private static final Logger log = LoggerFactory.getLogger(PersistedConfigRepo.class);
    private final Map<String, Application> idToApplication;
    private final Map<String, ServiceConfig> serviceConfigs;
    private final Map<String, String> applicationIdToServiceConfigIdMapping;
    private final Map<String, String> clientIdToServiceConfigIdMapping;
    
    private DB db;

    @Autowired
    public PersistedConfigRepo(
            @Value("${mapdb.path}") String mapDbPath) {
        File mapDbPathFile = new File(mapDbPath);
        log.debug("Using MapDB from {}", mapDbPathFile.getAbsolutePath());
        mapDbPathFile.getParentFile().mkdirs();
    	db = DBMaker.newFileDB(mapDbPathFile).make();
    	
        this.idToApplication = db.getHashMap("idToApplication");
        this.serviceConfigs = db.getHashMap("serviceConfigs");
        this.applicationIdToServiceConfigIdMapping = db.getHashMap("applicationIdToServiceConfigIdMapping");
        this.clientIdToServiceConfigIdMapping = db.getHashMap("clientIdToServiceConfigIdMapping");
    }

    @Override
    public Application createApplication(Application newApplication) {
        newApplication.id = UUID.randomUUID().toString();
        idToApplication.put(newApplication.id, newApplication);
        db.commit();
        return newApplication;
    }

    @Override
    public ServiceConfig createServiceConfig(String applicationId, ServiceConfig newServiceConfig) {
        newServiceConfig.setId(UUID.randomUUID().toString());
        serviceConfigs.put(newServiceConfig.getId(), newServiceConfig);
        applicationIdToServiceConfigIdMapping.put(applicationId, newServiceConfig.getId());
        db.commit();
        return newServiceConfig;
    }

    @Override
    public ServiceConfig getServiceConfig(String serviceConfigId) {
        return serviceConfigs.get(serviceConfigId);
    }

    @Override
    public ServiceConfig deleteServiceConfig(String serviceConfigId) {
    	ServiceConfig config = serviceConfigs.remove(serviceConfigId);
    	db.commit();
        return config;
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
        db.commit();
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
    	ServiceConfig config = serviceConfigs.put(newServiceConfig.getId(), newServiceConfig);
    	db.commit();
        return config;
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

    public void addOrUpdateConfig(String applicationId, ServiceConfig serviceConfig) {
        String serviceConfigId = serviceConfig.getId();
        if (serviceConfigId == null) {
            ServiceConfig persistedServiceConfig = createServiceConfig(applicationId, serviceConfig);
            serviceConfigId = persistedServiceConfig.getId();
        } else {
            updateServiceConfig(serviceConfig);
        }

        applicationIdToServiceConfigIdMapping.put(applicationId, serviceConfigId);
        db.commit();
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

    @Override
    public ServiceConfig changeServiceConfigForClientToUse(String clientId, String serviceConfigId) {
        ServiceConfig serviceConfig = serviceConfigs.get(serviceConfigId);
        if (serviceConfig != null) {
            clientIdToServiceConfigIdMapping.put(clientId, serviceConfigId);
            db.commit();
        }
        return serviceConfig;
    }

    @Override
    public Map<String, ServiceConfig> getAllServiceConfigs() {
        return serviceConfigs;
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
