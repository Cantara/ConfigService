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

    List<ApplicationConfig> findAllApplicationConfigsByArtifactId(String artifactId);
    
    ApplicationConfig findTheLatestApplicationConfigByArtifactId(String artifactId);

    List<ApplicationConfig> findAllApplicationConfigsByApplicationId(String applicationId);
    
    ApplicationConfig findTheLatestApplicationConfigByApplicationId(String applicationId);

    ApplicationConfig getApplicationConfig(String configId);

    ApplicationConfig deleteApplicationConfig(String configId);

    ApplicationConfig updateApplicationConfig(String configId, ApplicationConfig updatedConfig);

    String getArtifactId(ApplicationConfig config);

    List< ApplicationConfig> getAllConfigs();

    List<Application> getApplications();

	Application deleteApplication(String applicationId);

	boolean canDeleteApplicationConfig(String configId);

	boolean canDeleteApplication(String applicationId);
}
