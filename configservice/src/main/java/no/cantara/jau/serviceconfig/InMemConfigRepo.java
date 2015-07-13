package no.cantara.jau.serviceconfig;

import no.cantara.jau.serviceconfig.dto.DownloadItem;
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
        NexusUrlBuilder urlBuilder = new NexusUrlBuilder("http://mvnrepo.cantara.no", "snapshots");
        String artifactId = "UserAdminService";
        String version = "2.1-SNAPSHOT";
        String packaging = "jar";
        String filename = artifactId + "-" + version + "." + packaging;
        String url = urlBuilder.build("net.whydah.identity", artifactId, version, packaging);

        ServiceConfig serviceConfig = new ServiceConfig("Service1-1.23");
        serviceConfig.addDownloadItem(new DownloadItem(url, "username", "passwordABC", filename));
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + filename);
        addOrUpdateConfig("clientid1", serviceConfig);
    }

    public void addOrUpdateConfig(String clientId, ServiceConfig serviceConfig) {
        repo.put(clientId, serviceConfig);
    }

    public ServiceConfig findConfig(String clientId) {
        return repo.get(clientId);
    }
}
