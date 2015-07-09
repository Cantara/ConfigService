package no.cantara.jau;

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
        addOrUpdateConfig("clientid1", new ServiceConfig("Service1-1.23"));

    }

    public void addOrUpdateConfig(String clientId, ServiceConfig serviceConfig) {
        repo.put(clientId, serviceConfig);
    }

    public ServiceConfig findConfig(String clientId) {
        return repo.get(clientId);
    }
}
