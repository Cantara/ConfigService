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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
@Deprecated
public class PersistedConfigRepo implements ApplicationConfigDao, ClientDao {
	private static final Logger log = LoggerFactory.getLogger(PersistedConfigRepo.class);
	private final Map<String, Application> idToApplication;
	private final Map<String, ApplicationConfig> configs;
	private final Map<String, Client> clients;
	private final Map<String, ClientAlias> clientAliases;
	private final Map<String, ClientHeartbeatData> clientHeartbeatDataMap;
	private final Map<String, ClientEnvironment> clientEnvironmentMap;
	private final Map<String, String> configIdToApplicationId;
	private final Map<String, String> ignoredClients;
	private final Map<String, Long> configIdToCreateTimeStamp;
	 

	private DB db;

	@Autowired
	public PersistedConfigRepo(String mapDbPath) {
		File mapDbPathFile = new File(mapDbPath);
		log.debug("Using MapDB from {}", mapDbPathFile.getAbsolutePath());
		mapDbPathFile.getParentFile().mkdirs();
		db = DBMaker.newFileDB(mapDbPathFile).make();

		this.idToApplication = db.getHashMap("idToApplication");
		this.configs = db.getHashMap("configs");
		this.clients = db.getHashMap("clients");
		this.clientAliases = db.getHashMap("clientAliases");
		this.configIdToApplicationId = db.getHashMap("configIdToApplicationIdMapping");
		this.clientHeartbeatDataMap = db.getHashMap("clientHeartbeatData");
		this.clientEnvironmentMap = db.getHashMap("clientEnvironment");
		this.ignoredClients = db.getHashMap("ignoredclients");
		this.configIdToCreateTimeStamp = db.getHashMap("configIdToCreateTimeStamp");
		StringBuilder dbInfo = new StringBuilder().append("MapDB entries:");
		for (String mapName : db.getAll().keySet()) {
			dbInfo.append("\n").append(db.getHashMap(mapName).size()).append(" ").append(mapName);
		}
		log.info(dbInfo.toString());
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
		configIdToApplicationId.put(newConfig.getId(), applicationId);
		configIdToCreateTimeStamp.put(newConfig.getId(), System.currentTimeMillis());
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
		configIdToApplicationId.remove(configId);
		configIdToCreateTimeStamp.remove(configId);
		String artifactId = getArtifactId(config);
		Application app = findApplication(artifactId);
		
		if(app!=null){
			//if there is no other configs belonging to this app, we just delete the app as well
			if(findAllApplicationConfigsByApplicationId(app.id)==null || findAllApplicationConfigsByApplicationId(app.id).size()==0) {
				idToApplication.remove(app.id);
			}
		}
		for(Client client : new ArrayList<>(getAllClients())){
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
		if(config!=null) {
			String artifactId = getArtifactId(config);
			if(artifactId!=null){
				Application app = findApplication(artifactId);
				if (app != null) {
					return canDeleteApplication(app.id);
				}
			}
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
		
		List<ApplicationConfig> configList = findAllApplicationConfigsByApplicationId(applicationId);
		for(ApplicationConfig ac : configList) {
			configIdToApplicationId.remove(ac.getId());
			configIdToCreateTimeStamp.remove(ac.getId());
			configs.remove(ac.getId());
			
			//also remove clients having this configuration
			for (Client client : new ArrayList<>(getAllClients())){
				if(client.applicationConfigId.equals(ac.getId())){
					clientHeartbeatDataMap.remove(client.clientId);
					clientEnvironmentMap.remove(client.clientId);
					clients.remove(client.clientId);
				}
			}
		}
		Application app = idToApplication.remove(applicationId);
		db.commit();
		return app;
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
		for(Entry<String, String> configId_appId_entry: configIdToApplicationId.entrySet()) {
			if(configId_appId_entry.getValue().equals(applicationId)) {
				appConfigList.add(configs.get(configId_appId_entry.getKey()));
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
//		Optional<Map.Entry<String, String>> match = configIdMappingToApplicationId.entrySet()
//				.stream()
//				.filter(entry -> entry.getValue().equals(config.getId()))
//				.findFirst();
//		if (match.isPresent()) {
//			Application application = idToApplication.get(match.get().getKey());
//			return application == null ? null : application.artifactId;
//		}
//		return null;
		if(config!=null) {
			String appId = configIdToApplicationId.get(config.getId());
			Application app = idToApplication.get(appId);
			return app == null ? null : app.artifactId;
		} else {
			return null;
		}
		
	}

	@Override
	public List<ApplicationConfig> getAllConfigs() {
		return new ArrayList<>(configs.values());
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

	@Override
	public List<ClientAlias> getAllClientAliases() {
		return clientAliases.values().stream().collect(Collectors.toList());
	}
	
	@Override
	public void saveClientAlias(ClientAlias ca) {
		
		clientAliases.put(ca.clientId, ca);
		db.commit();
	}

	@Override
	public List<String> getAllIgnoredClientIds() {
		// TODO Auto-generated method stub
		return ignoredClients.values().stream().collect(Collectors.toList());
	}

	@Override
	public void saveIgnoredFlag(String clientId, boolean ignored) {
		if(ignored) {
			if(!ignoredClients.containsKey(clientId)) {
				ignoredClients.put(clientId, clientId);
			}
		} else {
			ignoredClients.remove(clientId);
		}
		db.commit();
	}

	@Override
	public ApplicationConfig findTheLatestApplicationConfigByArtifactId(String artifactId) {
		Application app=findApplication(artifactId);
		if(app!=null) {
			return findTheLatestApplicationConfigByApplicationId(app.id);
		}
		return null;
	}
	

	@Override
	public ApplicationConfig findTheLatestApplicationConfigByApplicationId(String applicationId) {
		
		List<ApplicationConfig> configList = findAllApplicationConfigsByApplicationId(applicationId);
		long time=0;
		ApplicationConfig theNewest =null; //which should have the latest creation time stamp
		for(ApplicationConfig config : configList) {
			if(configIdToCreateTimeStamp.get(config.getId()) > time) {
				time = configIdToCreateTimeStamp.get(config.getId());
				theNewest = config;
			}
		}
		return theNewest;
	}
	

	
}
