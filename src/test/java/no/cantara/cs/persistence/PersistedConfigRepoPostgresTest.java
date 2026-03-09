package no.cantara.cs.persistence;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.Client;
import no.cantara.cs.embedded.EmbeddedPostgresStingraySqlDatasource;
import no.cantara.stingray.sql.StingrayFlywayMigrationHelper;
import no.cantara.stingray.sql.StingraySqlDatasource;
import no.cantara.stingray.sql.StingraySqlDatasourceFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.sql.DataSource;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for the DAO methods that use the deprecated new Object[]{} pattern.
 *
 * Each test covers one of the deprecated jdbcTemplate calls (lines 121, 132, 144,
 * 191+197, 386, 438, 448 in PersistedConfigRepoPostgres). The tests verify correct
 * behaviour before and after the varargs modernisation refactor.
 */
public class PersistedConfigRepoPostgresTest {

    private StingraySqlDatasource stingraySqlDatasource;
    private PersistedConfigRepoPostgres repo;
    private JdbcTemplate jdbcTemplate;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @BeforeClass
    public void setUpDatabase() {
        ApplicationProperties config = ApplicationProperties.builder()
                .classpathPropertiesFile("embedded.properties")
                .build();
        createDatabase(config);
        grantSchemaPermissions(config);  // ensure flyway user can CREATE in public schema (PostgreSQL 15+)
        migrateDatabase(config);
        stingraySqlDatasource = ProviderLoader.configure(
                config.subTree("database.config"), "embedded", StingraySqlDatasourceFactory.class);
        DataSource dataSource = stingraySqlDatasource.getDataSource();
        jdbcTemplate = new JdbcTemplate(dataSource);
        repo = new PersistedConfigRepoPostgres(jdbcTemplate);
    }

    @AfterClass
    public void tearDown() {
        if (stingraySqlDatasource != null) {
            stingraySqlDatasource.close();
        }
    }

    @BeforeMethod
    public void cleanData() {
        // Only clean the tables exercised by this test class.
        // Existence-checked to survive both orderings (our migrations vs Spring migrations first).
        if (tableExists("client_environments"))   jdbcTemplate.update("DELETE FROM client_environments");
        if (tableExists("client_heartbeat_data")) jdbcTemplate.update("DELETE FROM client_heartbeat_data");
        if (tableExists("clients"))               jdbcTemplate.update("DELETE FROM clients");
        if (tableExists("application_configs"))   jdbcTemplate.update("DELETE FROM application_configs");
        if (tableExists("applications"))          jdbcTemplate.update("DELETE FROM applications");
    }

    private boolean tableExists(String tableName) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = 'public' AND table_name = ?",
                    String.class, tableName) != null;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Application givenApplication(String artifactId) {
        return repo.createApplication(new Application(artifactId));
    }

    private ApplicationConfig givenConfig(String applicationId, String name) {
        return repo.createApplicationConfig(applicationId, new ApplicationConfig(name));
    }

    // -----------------------------------------------------------------------
    // Tests — one per deprecated new Object[]{} call site
    // -----------------------------------------------------------------------

    // line 121: findAllApplicationConfigsByArtifactId
    //   jdbcTemplate.queryForObject("SELECT id from applications WHERE artifact_id = ?",
    //       new Object[]{artifactId}, String.class)

    @Test
    public void findAllApplicationConfigsByArtifactId_returnsAllConfigs() {
        Application app = givenApplication("test-artifact");
        givenConfig(app.id, "config-v1");
        givenConfig(app.id, "config-v2");

        List<ApplicationConfig> result = repo.findAllApplicationConfigsByArtifactId("test-artifact");

        assertNotNull(result);
        assertEquals(result.size(), 2);
    }

    @Test
    public void findAllApplicationConfigsByArtifactId_unknownArtifact_returnsNull() {
        assertNull(repo.findAllApplicationConfigsByArtifactId("no-such-artifact"));
    }

    // line 132: findAllApplicationConfigsByApplicationId
    //   jdbcTemplate.query("SELECT * FROM application_configs WHERE application_id = ? ...",
    //       new Object[] {applicationId}, PersistedConfigRepoPostgres::mapApplicationConfigList)

    @Test
    public void findAllApplicationConfigsByApplicationId_returnsAllConfigs() {
        Application app = givenApplication("app-by-id");
        givenConfig(app.id, "c1");
        givenConfig(app.id, "c2");
        givenConfig(app.id, "c3");

        List<ApplicationConfig> result = repo.findAllApplicationConfigsByApplicationId(app.id);

        assertNotNull(result);
        assertEquals(result.size(), 3);
    }

    @Test
    public void findAllApplicationConfigsByApplicationId_noConfigs_returnsEmptyList() {
        Application app = givenApplication("empty-app");

        List<ApplicationConfig> result = repo.findAllApplicationConfigsByApplicationId(app.id);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // line 144: getApplicationConfig
    //   jdbcTemplate.queryForObject("SELECT * from application_configs WHERE id = ?",
    //       new Object[]{configId}, ...)

    @Test
    public void getApplicationConfig_returnsConfig() {
        Application app = givenApplication("app-get-config");
        ApplicationConfig created = givenConfig(app.id, "my-config");

        ApplicationConfig result = repo.getApplicationConfig(created.getId());

        assertNotNull(result);
        assertEquals(result.getId(), created.getId());
        assertEquals(result.getName(), "my-config");
    }

    @Test
    public void getApplicationConfig_unknownId_returnsNull() {
        assertNull(repo.getApplicationConfig("no-such-id"));
    }

    // lines 191 + 197: getArtifactId (public) → getFirstFoundArtifactIdByApplicationId (private)
    //   line 191: jdbcTemplate.queryForObject("SELECT application_id FROM application_configs WHERE id = ? LIMIT 1",
    //       new Object[]{config.getId()}, String.class)
    //   line 197: jdbcTemplate.queryForObject("SELECT artifact_id FROM applications where id = ? LIMIT 1",
    //       new Object[]{applicationId}, String.class)

    @Test
    public void getArtifactId_returnsArtifactId() {
        Application app = givenApplication("artifact-lookup");
        ApplicationConfig config = givenConfig(app.id, "cfg");

        String artifactId = repo.getArtifactId(config);

        assertEquals(artifactId, "artifact-lookup");
    }

    // line 386: getAllClientsByConfigId
    //   jdbcTemplate.query("SELECT * FROM clients WHERE application_config_id = ?",
    //       new Object[] {configId}, ...)

    @Test
    public void getAllClientsByConfigId_returnsClients() {
        Application app = givenApplication("app-clients");
        ApplicationConfig config = givenConfig(app.id, "cfg");
        repo.saveClient(new Client("client-1", config.getId(), false));
        repo.saveClient(new Client("client-2", config.getId(), true));

        List<Client> clients = repo.getAllClientsByConfigId(config.getId());

        assertNotNull(clients);
        assertEquals(clients.size(), 2);
    }

    @Test
    public void getAllClientsByConfigId_noClients_returnsEmptyList() {
        Application app = givenApplication("app-no-clients");
        ApplicationConfig config = givenConfig(app.id, "cfg");

        List<Client> clients = repo.getAllClientsByConfigId(config.getId());

        assertNotNull(clients);
        assertTrue(clients.isEmpty());
    }

    // line 438: findTheLatestApplicationConfigByArtifactId
    //   jdbcTemplate.queryForObject("SELECT id from applications WHERE artifact_id = ?",
    //       new Object[]{artifactId}, String.class)

    @Test
    public void findTheLatestApplicationConfigByArtifactId_returnsSomeConfig() {
        Application app = givenApplication("latest-by-artifact");
        givenConfig(app.id, "only-config");

        ApplicationConfig result = repo.findTheLatestApplicationConfigByArtifactId("latest-by-artifact");

        assertNotNull(result);
    }

    @Test
    public void findTheLatestApplicationConfigByArtifactId_unknownArtifact_returnsNull() {
        assertNull(repo.findTheLatestApplicationConfigByArtifactId("ghost-artifact"));
    }

    // line 448: findTheLatestApplicationConfigByApplicationId
    //   jdbcTemplate.queryForObject("SELECT * from application_configs WHERE application_id = ? ORDER BY created_timestamp DESC LIMIT 1",
    //       new Object[]{applicationId}, ...)

    @Test
    public void findTheLatestApplicationConfigByApplicationId_returnsSomeConfig() {
        Application app = givenApplication("latest-by-app-id");
        ApplicationConfig config = givenConfig(app.id, "solo-config");

        ApplicationConfig result = repo.findTheLatestApplicationConfigByApplicationId(app.id);

        assertNotNull(result);
        assertEquals(result.getId(), config.getId());
    }

    @Test
    public void findTheLatestApplicationConfigByApplicationId_noConfigs_returnsNull() {
        Application app = givenApplication("empty-for-latest");

        assertNull(repo.findTheLatestApplicationConfigByApplicationId(app.id));
    }

    // -----------------------------------------------------------------------
    // Flyway bootstrap (mirrors EmbeddedPostgresStingraySqlDatasourceTest
    // + TestServerEmbedded.grantPermissions for PostgreSQL 15+ compatibility)
    // -----------------------------------------------------------------------

    private void createDatabase(ApplicationProperties config) {
        StingraySqlDatasource ds = ProviderLoader.configure(
                config.subTree("flyway.creation.config"), "embedded", StingraySqlDatasourceFactory.class);
        StingrayFlywayMigrationHelper.defaultCreation(
                "embeddedpgtest", ds,
                config.get("flyway.migration.config.dataSource.databaseName"),
                config.get("flyway.migration.config.dataSource.user"),
                config.get("flyway.migration.config.dataSource.password"),
                config.get("database.config.dataSource.databaseName"),
                config.get("database.config.dataSource.user"),
                config.get("database.config.dataSource.password")
        ).upgradeDatabase();
        ds.close();
    }

    /**
     * In PostgreSQL 15+, CREATE on the public schema is no longer granted to PUBLIC by default.
     * Connect as the postgres superuser and grant schema CREATE to the migration and app users.
     * All GRANTs are idempotent — safe to call even if permissions were already set by another test class.
     */
    private void grantSchemaPermissions(ApplicationProperties config) {
        StingraySqlDatasource creationDs = ProviderLoader.configure(
                config.subTree("flyway.creation.config"), "embedded", StingraySqlDatasourceFactory.class);
        EmbeddedPostgresStingraySqlDatasource embeddedDs = (EmbeddedPostgresStingraySqlDatasource) creationDs;
        DataSource superDataSource = embeddedDs.getEmbeddedPostgres()
                .getDatabase("postgres", config.get("database.config.dataSource.databaseName"));
        JdbcTemplate superJdbc = new JdbcTemplate(superDataSource);
        String migrationUser = config.get("flyway.migration.config.dataSource.user");
        String appUser = config.get("database.config.dataSource.user");
        superJdbc.execute("GRANT ALL ON SCHEMA public TO " + migrationUser);
        superJdbc.execute("GRANT ALL ON SCHEMA public TO " + appUser);
        superJdbc.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + migrationUser);
        superJdbc.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + appUser);
        superJdbc.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO " + migrationUser);
        superJdbc.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO " + appUser);
        // creationDs.close() is a no-op for the embedded postgres singleton
    }

    private void migrateDatabase(ApplicationProperties config) {
        StingraySqlDatasource ds = ProviderLoader.configure(
                config.subTree("flyway.migration.config"), "embedded", StingraySqlDatasourceFactory.class);
        StingrayFlywayMigrationHelper.forMigration(
                "db/migration", ds,
                config.get("database.config.dataSource.user")
        ).upgradeDatabase();
        ds.close();
    }
}
