package no.cantara.jau.persistence;

import no.cantara.jau.serviceconfig.dto.DownloadItem;
import no.cantara.jau.serviceconfig.dto.MavenMetadata;
import no.cantara.jau.serviceconfig.dto.NexusUrlBuilder;
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
@Service
public class InMemConfigRepo implements ServiceConfigDao {
    private final Map<String, ServiceConfig> serviceConfigs;
    private final Map<String, String> artifactIdToServiceConfigIdMapping;
    private final Map<String, String> clientIdToServiceConfigIdMapping;


    public InMemConfigRepo() {
        this.serviceConfigs = new HashMap<>();
        this.artifactIdToServiceConfigIdMapping = new HashMap<>();
        this.clientIdToServiceConfigIdMapping = new HashMap<>();
        addTestData();
    }

    @Override
    public ServiceConfig create(ServiceConfig newServiceConfig) {
        newServiceConfig.setId(UUID.randomUUID().toString());
        serviceConfigs.put(newServiceConfig.getId(), newServiceConfig);
        return newServiceConfig;
    }

    @Override
    public ServiceConfig get(String serviceConfigId) {
        return serviceConfigs.get(serviceConfigId);
    }

    @Override
    public ServiceConfig delete(String serviceConfigId) {
        return serviceConfigs.remove(serviceConfigId);
    }

    @Override
    public ServiceConfig findByArtifactId(String artifactId) {
        String serviceConfigId = artifactIdToServiceConfigIdMapping.get(artifactId);
        if (serviceConfigId == null) {
            return null;
        }
        return serviceConfigs.get(serviceConfigId);
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

    public void update(ServiceConfig newServiceConfig) {
        serviceConfigs.put(newServiceConfig.getId(), newServiceConfig);
    }

    public void addOrUpdateConfig(String artifactId, ServiceConfig serviceConfig) {
        String serviceConfigId = serviceConfig.getId();
        if (serviceConfigId == null) {
            ServiceConfig persistedServiceConfig = create(serviceConfig);
            serviceConfigId = persistedServiceConfig.getId();
        } else {
            update(serviceConfig);
        }

        artifactIdToServiceConfigIdMapping.put(artifactId, serviceConfigId);
    }

    //Should probably be moved to somewhere else.
    @Deprecated
    public ServiceConfig findConfig(String clientId) {
        String serviceConfigId = artifactIdToServiceConfigIdMapping.get(clientId);
        if (serviceConfigId == null) {
            return null;
        }
        return serviceConfigs.get(serviceConfigId);
    }


    private void addTestData() {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.1-SNAPSHOT");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "snapshots").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);

        ServiceConfig serviceConfig = new ServiceConfig("Service1-1.23");
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());
        addOrUpdateConfig("UserAdminService", serviceConfig);
    }
}
