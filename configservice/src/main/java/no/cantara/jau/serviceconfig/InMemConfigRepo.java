package no.cantara.jau.serviceconfig;

import no.cantara.jau.serviceconfig.dto.DownloadItem;
import no.cantara.jau.serviceconfig.dto.MavenMetadata;
import no.cantara.jau.serviceconfig.dto.NexusUrlBuilder;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
@Service
public class InMemConfigRepo implements ServiceConfigDao {
    private final Map<String, ServiceConfig> repo;

    public InMemConfigRepo() {
        this.repo = new HashMap<>();
        addTestData();
    }

    private void addTestData() {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.1-SNAPSHOT");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "snapshots").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);

        ServiceConfig serviceConfig = new ServiceConfig("Service1-1.23");
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());
        addOrUpdateConfig("clientid1", serviceConfig);
    }


    public void addOrUpdateConfig(String clientId, ServiceConfig serviceConfig) {
        repo.put(clientId, serviceConfig);
    }

    public ServiceConfig findConfig(String clientId) {
        return repo.get(clientId);
    }
}
