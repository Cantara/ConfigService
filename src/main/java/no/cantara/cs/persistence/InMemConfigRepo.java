package no.cantara.cs.persistence;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.ApplicationStatus;
import no.cantara.cs.dto.Client;
import no.cantara.cs.dto.ClientHeartbeatData;

import java.util.*;

/**
 * This class is a mess. Should be totally redesigned after the public API is stable.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
//Add @Service and remove @Service from PersistedConfigRepo to activate.
public class InMemConfigRepo implements ApplicationConfigDao {
    private final Map<String, Application> idToApplication;
    private final Map<String, ApplicationConfig> configs;
    private final Map<String, Client> clients;
    private final Map<String, String> applicationIdToConfigIdMapping;


    public InMemConfigRepo() {
        this.idToApplication = new HashMap<>();
        this.configs = new HashMap<>();
        this.applicationIdToConfigIdMapping = new HashMap<>();
        this.clients = new HashMap<>();
        //addTestData();
    }

    @Override
    public Application createApplication(Application newApplication) {
        newApplication.id = UUID.randomUUID().toString();
        idToApplication.put(newApplication.id, newApplication);
        return newApplication;
    }

    @Override
    public ApplicationConfig createApplicationConfig(String applicationId, ApplicationConfig newConfig) {
        newConfig.setId(UUID.randomUUID().toString());
        configs.put(newConfig.getId(), newConfig);
        applicationIdToConfigIdMapping.put(applicationId, newConfig.getId());
        return newConfig;
    }

    @Override
    public ApplicationConfig getApplicationConfig(String configId) {
        return configs.get(configId);
    }

    @Override
    public ApplicationConfig findApplicationConfigByArtifactId(String artifactId) {
        Application application = findApplication(artifactId);
        if (application == null) {
            return null;
        }

        return findApplicationConfigByApplicationId(application.id);
    }

    @Override
    public ApplicationConfig findApplicationConfigByApplicationId(String applicationId) {
        String configId = applicationIdToConfigIdMapping.get(applicationId);
        if (configId == null) {
            return null;
        }
        return configs.get(configId);
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
    public ApplicationConfig updateApplicationConfig(ApplicationConfig updatedConfig) {
        return configs.put(updatedConfig.getId(), updatedConfig);
    }

    @Override
    public String getArtifactId(ApplicationConfig config) {
        Optional<Map.Entry<String, String>> match = applicationIdToConfigIdMapping.entrySet()
                                                                                  .stream()
                                                                                  .filter(entry -> entry.getValue().equals(config.getId()))
                                                                                  .findFirst();
        if (match.isPresent()) {
            Application application = idToApplication.get(match.get().getKey());
            return application.artifactId;
        }
        return null;
    }

    @Override
    public Map<String, ApplicationConfig> getAllConfigs() {
        //TODO: Implementation
        return null;
    }

    @Override
    public List<Application> getApplications() {
        return new ArrayList<>(idToApplication.values());
    }

   
    @Override
	public ApplicationConfig deleteApplicationConfig(String configId) {
    	ApplicationConfig config = configs.remove(configId);
		String artifactId = getArtifactId(config);
		Application app = findApplication(artifactId);
		if(app!=null){
			idToApplication.remove(app.id);
			applicationIdToConfigIdMapping.remove(app.id);
		}
		return config;
	}

	@Override
	public boolean canDeleteApplicationConfig(String configId) {
		return true;
	}

	@Override
	public boolean canDeleteApplication(String applicationId) {
		return true;
	}

	@Override
	public Application deleteApplication(String applicationId) {
		Application app = idToApplication.remove(applicationId);
		String configId =applicationIdToConfigIdMapping.remove(applicationId);
		configs.remove(configId);
		return app;
	}

}
