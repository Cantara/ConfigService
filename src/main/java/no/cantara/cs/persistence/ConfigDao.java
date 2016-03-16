package no.cantara.cs.persistence;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.Client;
import no.cantara.cs.dto.Config;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public interface ConfigDao {
    Application createApplication(Application newApplication);

    Config createConfig(String applicationId, Config newConfig);

    Config findConfigByArtifactId(String artifactId);

    Config getConfig(String configId);

    Config deleteConfig(String configId);

    Config updateConfig(Config updatedConfig);

    String getArtifactId(Config config);

    Map<String, Config> getAllConfigs();

    List<Application> getApplications();
}
