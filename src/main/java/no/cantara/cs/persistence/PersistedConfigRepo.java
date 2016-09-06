package no.cantara.cs.persistence;

import no.cantara.cs.dto.*;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
@Service
public class PersistedConfigRepo implements ApplicationConfigDao, ClientDao {
	private static final Logger log = LoggerFactory.getLogger(PersistedConfigRepo.class);
	private final Map<String, Application> idToApplication;
	private final Map<String, ApplicationConfig> configs;
	private final Map<String, Client> clients;
	private final Map<String, ClientHeartbeatData> clientHeartbeatDataMap;
	private final Map<String, ClientEnvironment> clientEnvironmentMap;
	private final Map<String, String> applicationIdToConfigIdMapping;

	private DB db;

	@Autowired
	public PersistedConfigRepo(
			@Value("${mapdb.path}") String mapDbPath) {
		File mapDbPathFile = new File(mapDbPath);
		log.debug("Using MapDB from {}", mapDbPathFile.getAbsolutePath());
		mapDbPathFile.getParentFile().mkdirs();
		db = DBMaker.newFileDB(mapDbPathFile).make();

		this.idToApplication = db.getHashMap("idToApplication");
		this.configs = db.getHashMap("configs");
		this.clients = db.getHashMap("clients");
		this.applicationIdToConfigIdMapping = db.getHashMap("applicationIdToConfigIdMapping");
		this.clientHeartbeatDataMap = db.getHashMap("clientHeartbeatData");
		this.clientEnvironmentMap = db.getHashMap("clientEnvironment");
	}

	@Override
	public Application createApplication(Application newApplication) {
		if (idToApplication.values().stream().anyMatch(a -> newApplication.artifactId.equals(a.artifactId))) {
			log.warn("CreateApplication with same artifactId already exists, artifactId: {}", newApplication.artifactId);
			throw new IllegalArgumentException("Application with same artifactId already exists, artifactId: " + newApplication.artifactId);
		}
		newApplication.id = UUID.randomUUID().toString();
		idToApplication.put(newApplication.id, newApplication);
		log.info("created {}", newApplication);
		db.commit();
		return newApplication;
	}

	@Override
	public ApplicationConfig createApplicationConfig(String applicationId, ApplicationConfig newConfig) {
		newConfig.setId(UUID.randomUUID().toString());
		configs.put(newConfig.getId(), newConfig);
		applicationIdToConfigIdMapping.put(applicationId, newConfig.getId());
		db.commit();
		return newConfig;
	}

	@Override
	public ApplicationConfig getApplicationConfig(String configId) {
		return configs.get(configId);
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
		for(Client client : new ArrayList<Client>(getAllClients())){
			if(client.applicationConfigId.equals(configId)){
				clientHeartbeatDataMap.remove(client.clientId);
				clientEnvironmentMap.remove(client.clientId);
				clients.remove(client.clientId);
			}
		}
		db.commit();
		return config;
	}

	@Override
	public boolean canDeleteApplicationConfig(String configId) {
		ApplicationConfig config = configs.get(configId);
		String artifactId = getArtifactId(config);
		if(artifactId!=null){
			return canDeleteApplication(findApplication(artifactId).id);
		}
		return true;
	}

	@Override
	public boolean canDeleteApplication(String applicationId) {
		Application app = idToApplication.get(applicationId);
		if(app!=null){
			Map<String, ClientHeartbeatData> clientStatuses = getAllClientHeartbeatData(app.artifactId);
			ApplicationStatus appStatuses = new ApplicationStatus(clientStatuses);
			if(appStatuses.seenInTheLastHourCount>0){
				return false;
			}
		}
		return true;
	}

	@Override
	public Application deleteApplication(String applicationId) {
		Application app = idToApplication.remove(applicationId);
		String configId =applicationIdToConfigIdMapping.remove(applicationId);
		configs.remove(configId);
		for(Client client : new ArrayList<Client>(getAllClients())){
			if(client.applicationConfigId.equals(configId)){
				clientHeartbeatDataMap.remove(client.clientId);
				clientEnvironmentMap.remove(client.clientId);
				clients.remove(client.clientId);
			}
		}
		
		db.commit();
		return app;
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
	public Client getClient(String clientId) {
		return clients.get(clientId);
	}

	@Override
	public void saveClient(Client client) {
		if (client.applicationConfigId == null) {
			throw new IllegalArgumentException("client.applicationConfigId is required");
		}
		// Verify applicationApplicationConfig exists
		ApplicationConfig config = getApplicationConfig(client.applicationConfigId);
		if (config == null) {
			throw new IllegalArgumentException("No ApplicationApplicationConfig was found with id: " + client.applicationConfigId);
		}
		clients.put(client.clientId, client);
		db.commit();
	}

	@Override
	public ApplicationConfig updateApplicationConfig(String configId, ApplicationConfig updatedConfig) {
		updatedConfig.setId(configId);
		ApplicationConfig config = configs.put(configId, updatedConfig);
		db.commit();
		return config;
	}

	@Override
	public String getArtifactId(ApplicationConfig config) {
		// Note: this code is a work-around for missing many-to-one mapping from configuration to application.
		Optional<Map.Entry<String, String>> match = applicationIdToConfigIdMapping.entrySet()
				.stream()
				.filter(entry -> entry.getValue().equals(config.getId()))
				.findFirst();
		if (match.isPresent()) {
			Application application = idToApplication.get(match.get().getKey());
			return application == null ? null : application.artifactId;
		}
		return null;
	}

	@Override
	public Map<String, ApplicationConfig> getAllConfigs() {
		return configs;
	}

	@Override
	public List<Application> getApplications() {
		return new ArrayList<>(idToApplication.values());
	}

	@Override
	public ClientHeartbeatData getClientHeartbeatData(String clientId) {
		return clientHeartbeatDataMap.get(clientId);
	}

	@Override
	public void saveClientHeartbeatData(String clientId, ClientHeartbeatData clientHeartbeatData) {
		clientHeartbeatDataMap.put(clientId, clientHeartbeatData);
	}

	@Override
	public void saveClientEnvironment(String clientId, ClientEnvironment clientEnvironment) {
		clientEnvironmentMap.put(clientId, clientEnvironment);
	}

	@Override
	public ClientEnvironment getClientEnvironment(String clientId) {
		return clientEnvironmentMap.get(clientId);
	}

	@Override
	public Map<String, ClientHeartbeatData> getAllClientHeartbeatData(String artifactId) {
		return clientHeartbeatDataMap.entrySet().stream()
				.filter(e -> artifactId.equals(e.getValue().artifactId))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public List<Client> getAllClients() {
		return clients.values().stream().collect(Collectors.toList());
	}
}
