package no.cantara.cs.persistence;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.InternalServerErrorException;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.ApplicationStatus;
import no.cantara.cs.dto.Client;
import no.cantara.cs.dto.ClientAlias;
import no.cantara.cs.dto.ClientEnvironment;
import no.cantara.cs.dto.ClientHeartbeatData;

@Transactional
public class PersistedConfigRepoPostgres implements ApplicationConfigDao, ClientDao {
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final Logger log = LoggerFactory.getLogger(PersistedConfigRepoPostgres.class);

	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public PersistedConfigRepoPostgres(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	private static ApplicationConfig mapApplicationConfig(ResultSet resultSet, int i) {
		try {
			return mapper.readValue(resultSet.getString("data"), ApplicationConfig.class);
		} catch (IOException | SQLException e) {
			log.error("Failed to lookup application config", e);
			throw new RuntimeException(e);
		}
	}

	private static List<ApplicationConfig> mapApplicationConfigList(ResultSet rs) {

		List<ApplicationConfig> results = new ArrayList<>();
		try {
			while (rs.next()) {
				ApplicationConfig appConfig = mapper.readValue(rs.getString("data"), ApplicationConfig.class);      
				results.add(appConfig);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return results;

	}

	private static List<Application> mapApplications(ResultSet rs) {
		List<Application> results = new ArrayList<>();
		try {
			while (rs.next()) {
				Application app = new Application(rs.getString("artifact_id"));
				app.id = rs.getString("id");
				results.add(app);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		return results;
	}

	@Override
	public Application createApplication(Application newApplication) {
		boolean exists = doesApplicationExist(newApplication);

		if (exists) {
			log.warn("CreateApplication with same artifactId already exists, artifactId: {}", newApplication.artifactId);
			throw new IllegalArgumentException("Application with same artifactId already exists, artifactId: " + newApplication.artifactId);
		}

		newApplication.id = UUID.randomUUID().toString();
		jdbcTemplate.update("INSERT INTO applications(id, artifact_id) VALUES (?,?)", newApplication.id, newApplication.artifactId);
		log.info("Created {}", newApplication);
		return newApplication;
	}

	private boolean doesApplicationExist(Application newApplication) {
		try {
			jdbcTemplate.queryForObject("SELECT id from applications WHERE artifact_id = ?", String.class,
					newApplication.artifactId);
			return true;
		} catch (EmptyResultDataAccessException e) {
			return false;
		}
	}

	@Override
	public ApplicationConfig createApplicationConfig(String applicationId, ApplicationConfig newConfig) {
		newConfig.setId(UUID.randomUUID().toString());
		newConfig.setUpdated();
		jdbcTemplate.update("INSERT INTO application_configs(id, application_id, data, created_timestamp) VALUES (?,?,?, now() at time zone 'utc')", newConfig.getId(), applicationId, pojoToJsonWrapper(newConfig));
		return newConfig;
	}

	@Override
	public List<ApplicationConfig> findAllApplicationConfigsByArtifactId(String artifactId) {
		try {
			String applicationId = jdbcTemplate.queryForObject("SELECT id from applications WHERE artifact_id = ?", new Object[]{artifactId}, String.class);
			return findAllApplicationConfigsByApplicationId(applicationId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public List<ApplicationConfig> findAllApplicationConfigsByApplicationId(String applicationId) {
		try {

			return jdbcTemplate.query("SELECT * FROM application_configs WHERE application_id = ? ORDER BY created_timestamp DESC", new Object[] {applicationId}, PersistedConfigRepoPostgres::mapApplicationConfigList);
			//        	
			//            return jdbcTemplate.queryForObject("SELECT * from application_configs WHERE application_id = ? ORDER BY created_timestamp DESC LIMIT 1", new Object[]{applicationId},
			//                    (PersistedConfigRepoPostgres::mapApplicationConfig));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public ApplicationConfig getApplicationConfig(String configId) {
		try {
			return jdbcTemplate.queryForObject("SELECT * from application_configs WHERE id = ?", new Object[]{configId},
					(PersistedConfigRepoPostgres::mapApplicationConfig));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public ApplicationConfig deleteApplicationConfig(String configId) {
		ApplicationConfig config = getApplicationConfig(configId);
		jdbcTemplate.update("DELETE FROM application_configs where id = ?", configId);
		jdbcTemplate.update("DELETE FROM clients where application_config_id = ?", configId);
		
		/*
		String artifactId = getArtifactId(config);
		if(artifactId!=null) {
			Application app = findApplicationByArtifactId(artifactId);
			if(app!=null) {
				ApplicationConfig theLatest = findTheLatestApplicationConfigByArtifactId(artifactId);
				if(theLatest==null) { 
					//no more config, so we can remove this application now
					deleteApplication(app.id);
				}
			}
			
		}*/
		
		getAllClients().stream()
		.filter(c -> c.applicationConfigId.equals(configId))
		.forEach(c -> {
			jdbcTemplate.update("DELETE FROM client_environment where client_id = ?", c.clientId);
			jdbcTemplate.update("DELETE FROM client_environment where application_config_id = ?", c.clientId);
		});
		return config;
	}

	@Override
	public ApplicationConfig updateApplicationConfig(String configId, ApplicationConfig updatedConfig) {
		updatedConfig.setId(configId);
		updatedConfig.setUpdated();
		jdbcTemplate.update("UPDATE application_configs SET data = ? WHERE id = ?", pojoToJsonWrapper(updatedConfig), configId);
		return updatedConfig;
	}

	@Override
	public String getArtifactId(ApplicationConfig config) {
		// Note: this code is a work-around for missing many-to-one mapping from configuration to application.
		String applicationId = jdbcTemplate.queryForObject("SELECT application_id FROM application_configs WHERE id = ? LIMIT 1", new Object[]{config.getId()}, String.class);
		return getFirstFoundArtifactIdByApplicationId(applicationId);
	}

	private String getFirstFoundArtifactIdByApplicationId(String applicationId) {
		try {
			return jdbcTemplate.queryForObject("SELECT artifact_id FROM applications where id = ? LIMIT 1", new Object[]{applicationId}, String.class);
		} catch (EmptyResultDataAccessException ignored) {
			return null;
		}
	}

	@Override
	public List<ApplicationConfig> getAllConfigs() {
		return jdbcTemplate.query("SELECT id, data FROM application_configs", PersistedConfigRepoPostgres::mapApplicationConfigList);
	}

	@Override
	public List<Application> getApplications() {
		return jdbcTemplate.query("SELECT * from applications", PersistedConfigRepoPostgres::mapApplications);
	}

	@Override
	public Application deleteApplication(String applicationId) {
		List<ApplicationConfig> configList = findAllApplicationConfigsByApplicationId(applicationId);
		for(ApplicationConfig config : configList) {
			deleteApplicationConfig(config.getId());
		}
		try {
		    List<Application> appList = jdbcTemplate.query("DELETE FROM applications WHERE id = ? RETURNING *", PersistedConfigRepoPostgres::mapApplications, applicationId);
		    if(appList.size()==0) {
		    	return null;
		    } else {
		    	return appList.get(0);
		    }
		} catch (RuntimeException e) {
			throw e;
		}
	}

	@Override
	public boolean canDeleteApplicationConfig(String configId) {
		return ofNullable(getApplicationConfig(configId))
				.map(appConfig -> getArtifactId(appConfig))
				.map(artifactId -> findApplicationByArtifactId(artifactId))
				.map(app -> app.id)
				.map(id -> canDeleteApplication(id))
				.orElse(true);
	}

	private Application findApplicationByArtifactId(String artifactId) {
		return jdbcTemplate.query("SELECT * from applications where artifact_id = ? LIMIT 1", (rs -> {
			if (rs.next()) {
				Application a = new Application(rs.getString("artifact_id"));
				a.id = rs.getString("id");
				return a;
			}
			return null;
		}), artifactId);
	}

	private Application findApplicationByApplicationId(String applicationId) {
		return jdbcTemplate.query("SELECT * from applications where id = ? LIMIT 1", (rs -> {
			if (rs.next()) {
				Application a = new Application(rs.getString("artifact_id"));
				a.id = rs.getString("id");
				return a;
			}
			return null;
		}), applicationId);
	}



	@Override
	public boolean canDeleteApplication(String applicationId) {
		return ofNullable(findApplicationByApplicationId(applicationId))
				.map(app -> app.artifactId)
				.map(artifactId -> getAllClientHeartbeatData(artifactId))
				.map(data -> new ApplicationStatus(data).seenInTheLastHourCount == 0)
				.orElse(true);
	}

	private static PGobject pojoToJsonWrapper(Object msg) {
		try {
			PGobject jsonObject = new PGobject();
			jsonObject.setType("jsonb");
			jsonObject.setValue(mapper.writeValueAsString(msg));
			return jsonObject;
		} catch (JsonProcessingException | SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJSON(String toDeserialize, Class<T> target) {
		try {
			return mapper.readValue(toDeserialize, target);
		} catch (IOException e) {
			log.error("Failed deserializing json={}", toDeserialize, e);
			throw new InternalServerErrorException();
		}
	}

	@Override
	public Client getClient(String clientId) {
		return jdbcTemplate.query("SELECT * FROM clients where client_id = ?", (rs -> {
			if (rs.next()) {
				return new Client(rs.getString("client_id"), rs.getString("application_config_id"),
						rs.getBoolean("auto_upgrade"));
			}
			return null;
		}), clientId);
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
		jdbcTemplate.update("INSERT INTO clients (client_id, application_config_id, auto_upgrade) VALUES (?,?,?) ON CONFLICT (client_id) " +
				"DO UPDATE SET application_config_id = EXCLUDED.application_config_id, auto_upgrade = EXCLUDED.auto_upgrade",
				client.clientId, client.applicationConfigId, client.autoUpgrade);
	}

	@Override
	public void saveClientHeartbeatData(String clientId, ClientHeartbeatData clientHeartbeatData) {
		jdbcTemplate.update("INSERT INTO client_heartbeat_data (client_id, data) VALUES (?,?) ON CONFLICT (client_id) DO UPDATE SET data = EXCLUDED.data",
				clientId, pojoToJsonWrapper(clientHeartbeatData));
	}

	@Override
	public ClientHeartbeatData getClientHeartbeatData(String clientId) {
		try {
			return fromJSON(jdbcTemplate.queryForObject("SELECT data FROM client_heartbeat_data WHERE client_id = ?", String.class, clientId), ClientHeartbeatData.class);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public void saveClientEnvironment(String clientId, ClientEnvironment clientEnvironment) {
		jdbcTemplate.update("INSERT INTO client_environments (client_id, data) VALUES (?,?) ON CONFLICT (client_id) DO UPDATE SET data = EXCLUDED.data",
				clientId, pojoToJsonWrapper(clientEnvironment));
	}

	@Override
	public ClientEnvironment getClientEnvironment(String clientId) {
		try {
			return fromJSON(jdbcTemplate.queryForObject("SELECT data FROM client_environments WHERE client_id = ?", String.class, clientId), ClientEnvironment.class);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public Map<String, ClientHeartbeatData> getAllClientHeartbeatData(String artifactId) {
		return jdbcTemplate.query("SELECT * FROM client_heartbeat_data WHERE data ->> 'artifactId' = ?", (rs -> {
			Map<String, ClientHeartbeatData> results = new HashMap<>();
			while (rs.next()) {
				results.put(rs.getString("client_id"), fromJSON(rs.getString("data"), ClientHeartbeatData.class));
			}
			return results;
		}), artifactId);
	}

	@Override
	public List<Client> getAllClients() {
		return jdbcTemplate.query("SELECT * FROM clients", (rs) -> {
			List<Client> results = new ArrayList<>();
			while (rs.next()) {
				results.add(new Client(rs.getString("client_id"), rs.getString("application_config_id"), rs.getBoolean("auto_upgrade")));
			}
			return results;
		});
	}

	@Override
	public List<ClientAlias> getAllClientAliases(){	 
		return jdbcTemplate.query("SELECT * FROM clientaliases", (rs) -> {
			List<ClientAlias> results = new ArrayList<>();
			while (rs.next()) {
				results.add(new ClientAlias(rs.getString("client_id"), rs.getString("client_name")));
			}
			return results;
		});
	}

	@Override
	public void saveClientAlias(ClientAlias ca) {
		jdbcTemplate.update("INSERT INTO clientaliases (client_id, client_name) VALUES (?,?) ON CONFLICT (client_id) " +
				"DO UPDATE SET client_name = EXCLUDED.client_name",
				ca.clientId, ca.clientName);
	}

	@Override
	public List<String> getAllIgnoredClientIds() {
		return jdbcTemplate.query("SELECT * FROM ignoredclients", (rs) -> {
			List<String> results = new ArrayList<>();
			while (rs.next()) {
				results.add(rs.getString("client_id"));
			}
			return results;
		});
	}

	@Override
	public void saveIgnoredFlag(String clientId, boolean ignored) {
		if(ignored) {
			jdbcTemplate.update("INSERT INTO ignoredclients (client_id) VALUES (?) ON CONFLICT (client_id) " +
					"DO NOTHING", clientId);
		} else {
			jdbcTemplate.update("DELETE FROM ignoredclients where client_id = ?", clientId);
		}		
	}


	@Override
	public ApplicationConfig findTheLatestApplicationConfigByArtifactId(String artifactId) {
		try {
			String applicationId = jdbcTemplate.queryForObject("SELECT id from applications WHERE artifact_id = ?", new Object[]{artifactId}, String.class);
			return findTheLatestApplicationConfigByApplicationId(applicationId);
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	@Override
	public ApplicationConfig findTheLatestApplicationConfigByApplicationId(String applicationId) {
		try {
			return jdbcTemplate.queryForObject("SELECT * from application_configs WHERE application_id = ? ORDER BY created_timestamp DESC LIMIT 1", new Object[]{applicationId},
					(PersistedConfigRepoPostgres::mapApplicationConfig));
		} catch (EmptyResultDataAccessException e) {
			return null;
		}
	}


}
