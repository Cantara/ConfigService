package no.cantara.cs.persistence;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.ApplicationStatus;
import no.cantara.cs.dto.Client;
import no.cantara.cs.dto.ClientHeartbeatData;

import java.util.*;
import java.util.Map.Entry;

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
    private final Map<String, String> ConfigIdToAppIdMapping;


    public InMemConfigRepo() {
        this.idToApplication = new LinkedHashMap<>();
        this.configs = new LinkedHashMap<>();
        this.ConfigIdToAppIdMapping = new LinkedHashMap<>();
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
        ConfigIdToAppIdMapping.put(applicationId, newConfig.getId());
        return newConfig;
    }

    @Override
    public ApplicationConfig getApplicationConfig(String configId) {
        return configs.get(configId);
    }

    @Override
    public List<ApplicationConfig> findAllApplicationConfigsByArtifactId(String artifactId) {
        Application application = findApplication(artifactId);
        if (application == null) {
            return null;
        }

        return findAllApplicationConfigsByApplicationId(application.id);
    }

    @Override
    public List<ApplicationConfig> findAllApplicationConfigsByApplicationId(String applicationId) {
        
        List<ApplicationConfig> appConfigList = new ArrayList<>();
        for(String configid : new ArrayList<>(ConfigIdToAppIdMapping.keySet())) {
        	if(ConfigIdToAppIdMapping.get(configid).equals(applicationId)) {
        		appConfigList.add(configs.get(configid));
        	}
        }
        return appConfigList;
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
    public ApplicationConfig updateApplicationConfig(String configId, ApplicationConfig updatedConfig) {
    	updatedConfig.setId(configId);
        return configs.put(updatedConfig.getId(), updatedConfig);
    }

    @Override
    public String getArtifactId(ApplicationConfig config) {
        Optional<Map.Entry<String, String>> match = ConfigIdToAppIdMapping.entrySet()
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
    public List<ApplicationConfig> getAllConfigs() {
        //TODO: Implementation
        return new ArrayList<>(configs.values());
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
			ConfigIdToAppIdMapping.remove(app.id);
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
		String configId =ConfigIdToAppIdMapping.remove(applicationId);
		configs.remove(configId);
		return app;
	}

	@Override
	public ApplicationConfig findTheLatestApplicationConfigByArtifactId(String artifactId) {
		Application app = findApplication(artifactId);
		return findTheLatestApplicationConfigByApplicationId(app.id);
	}

	@Override
	public ApplicationConfig findTheLatestApplicationConfigByApplicationId(String applicationId) {
		Entry<String, String> last = null;
		for (Entry<String, String> e : ConfigIdToAppIdMapping.entrySet()) {
			if(e.getValue().equals(applicationId)) {
				last = e;
			}
		}
		return configs.get(last.getKey());
		
	}

	@Override
	public Application getApplication(String artifact) {
		for (Entry<String, Application> e : idToApplication.entrySet()) {
			if(e.getValue().artifactId.toLowerCase().equals(artifact.toLowerCase())) {
				return e.getValue();
			}
		}
		return null;
	}

}
