package no.cantara.cs.persistence;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.Config;

import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public interface ConfigDao {
    Application createApplication(Application newApplication);


    void addOrUpdateConfig(String clientId, Config config);
    Config findConfig(String clientId);

    Config createConfig(String applicationId, Config newConfig);

    Config findByArtifactId(String artifactId);

    void registerClient(String clientId, String configId);
    Config findByClientId(String clientId);

    Config getConfig(String configId);

    Config deleteConfig(String configId);

    Config updateConfig(Config updatedConfig);

    String getArtifactId(Config config);

    Config changeConfigForClientToUse(String clientId, String configId);

    Map<String, Config> getAllConfigs();
}
