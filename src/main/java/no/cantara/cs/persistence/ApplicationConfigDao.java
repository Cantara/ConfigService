package no.cantara.cs.persistence;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;

import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public interface ApplicationConfigDao {
    Application createApplication(Application newApplication);

    ApplicationConfig createApplicationConfig(String applicationId, ApplicationConfig newConfig);

    ApplicationConfig findApplicationConfigByArtifactId(String artifactId);

    ApplicationConfig findApplicationConfigByApplicationId(String applicationId);

    ApplicationConfig getApplicationConfig(String configId);

    ApplicationConfig deleteApplicationConfig(String configId);

    ApplicationConfig updateApplicationConfig(ApplicationConfig updatedConfig);

    String getArtifactId(ApplicationConfig config);

    Map<String, ApplicationConfig> getAllConfigs();

    List<Application> getApplications();

	Application deleteApplication(String applicationId);

	boolean canDeleteApplicationConfig(String configId);

	boolean canDeleteApplication(String applicationId);
}
