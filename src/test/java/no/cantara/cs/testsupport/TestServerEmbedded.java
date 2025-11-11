package no.cantara.cs.testsupport;

import io.restassured.RestAssured;
import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.cs.Main;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.embedded.EmbeddedPostgresStingraySqlDatasource;
import no.cantara.stingray.sql.StingrayFlywayMigrationHelper;
import no.cantara.stingray.sql.StingraySqlDatasource;
import no.cantara.stingray.sql.StingraySqlDatasourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

public class TestServerEmbedded implements TestServer{

    private static final Logger log = LoggerFactory.getLogger(TestServerEmbedded.class);

    private Main main;
    private String url;
    private Class testClass;

    public TestServerEmbedded(Class testClass) {
        this.testClass = testClass;
    }

    @Override
    public void cleanAllData() throws Exception {
        ApplicationProperties config = ApplicationProperties.builder()
                .classpathPropertiesFile("embedded.properties")
                .build();
        createDatabase(config);
        grantPermissions(config);  // Grant permissions but don't run migrations yet
        // migrateDatabase(config);  // REMOVED - Let SpringConfigEmbedded handle migrations

        // Connect to clean data only if tables exist
        StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("database.config"), "embedded", StingraySqlDatasourceFactory.class);
        try {
            DataSource dataSource = stingraySqlDatasource.getDataSource();
            try (Connection connection = dataSource.getConnection()) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                if (tableExists(jdbcTemplate, "applications")) {
                    jdbcTemplate.update("DELETE FROM applications");
                }
                if (tableExists(jdbcTemplate, "application_configs")) {
                    jdbcTemplate.update("DELETE FROM application_configs");
                }
                if (tableExists(jdbcTemplate, "clients")) {
                    jdbcTemplate.update("DELETE FROM clients");
                }
                if (tableExists(jdbcTemplate, "client_heartbeat_data")) {
                    jdbcTemplate.update("DELETE FROM client_heartbeat_data");
                }
                if (tableExists(jdbcTemplate, "client_environments")) {
                    jdbcTemplate.update("DELETE FROM client_environments");
                }
            }
        } finally {
            stingraySqlDatasource.close();
        }
    }

    @Override
    public void start() throws InterruptedException {
        new Thread(() -> {
            main = new Main("embedded");
            main.start();
        }).start();
        do {
            Thread.sleep(10);
        } while (main == null || !main.isStarted());
        RestAssured.port = main.getPort();

        RestAssured.basePath = Main.CONTEXT_PATH;
        url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH + ClientResource.CLIENT_PATH;
    }

    @Override
    public void stop() {
        main.stop();
    }

    @Override
    public ConfigServiceClient getConfigServiceClient() {
        return new ConfigServiceClient(url, USERNAME, PASSWORD).withApplicationStateFilename("target/" + testClass.getSimpleName() + ".properties");
    }

    @Override
    public ConfigServiceAdminClient getAdminClient() {
        return new ConfigServiceAdminClient("http://localhost:" + main.getPort() + Main.CONTEXT_PATH, TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD);
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            return jdbcTemplate.queryForObject("SELECT table_name FROM information_schema.tables "
                    + "WHERE table_schema='public' and table_name = ?", String.class, tableName)
                    != null;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    private void createDatabase(ApplicationProperties config) {
        StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("flyway.creation.config"), "embedded", StingraySqlDatasourceFactory.class);
        StingrayFlywayMigrationHelper flywayMigrationHelper = StingrayFlywayMigrationHelper.defaultCreation(
                "embeddedpgtest", stingraySqlDatasource,
                config.get("flyway.migration.config.dataSource.databaseName"),
                config.get("flyway.migration.config.dataSource.user"),
                config.get("flyway.migration.config.dataSource.password"),
                config.get("database.config.dataSource.databaseName"),
                config.get("database.config.dataSource.user"),
                config.get("database.config.dataSource.password")
        );
        flywayMigrationHelper.upgradeDatabase();
        stingraySqlDatasource.close();
    }

    private void grantPermissions(ApplicationProperties config) {
        // Connect to the target database (junit) as the postgres user to grant permissions
        StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("flyway.creation.config"), "embedded", StingraySqlDatasourceFactory.class);

        try {
            // Cast to EmbeddedPostgresStingraySqlDatasource to access getEmbeddedPostgres()
            if (!(stingraySqlDatasource instanceof EmbeddedPostgresStingraySqlDatasource)) {
                throw new IllegalStateException("Expected EmbeddedPostgresStingraySqlDatasource but got " + stingraySqlDatasource.getClass().getName());
            }

            EmbeddedPostgresStingraySqlDatasource embeddedDatasource = (EmbeddedPostgresStingraySqlDatasource) stingraySqlDatasource;

            // Get the embedded postgres instance and connect to the junit database
            DataSource dataSource = embeddedDatasource.getEmbeddedPostgres().getDatabase("postgres", config.get("database.config.dataSource.databaseName"));

            try (Connection connection = dataSource.getConnection()) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                String migrationUser = config.get("flyway.migration.config.dataSource.user");
                String appUser = config.get("database.config.dataSource.user");

                // Grant schema usage and create permissions on the junit database
                jdbcTemplate.execute("GRANT ALL ON SCHEMA public TO " + migrationUser);
                jdbcTemplate.execute("GRANT ALL ON SCHEMA public TO " + appUser);
                jdbcTemplate.execute("GRANT ALL ON DATABASE " + config.get("database.config.dataSource.databaseName") + " TO " + migrationUser);
                jdbcTemplate.execute("GRANT ALL ON DATABASE " + config.get("database.config.dataSource.databaseName") + " TO " + appUser);

                // Also grant default privileges for future tables
                jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + migrationUser);
                jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO " + appUser);
                jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO " + migrationUser);
                jdbcTemplate.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO " + appUser);

                log.info("Granted permissions on public schema in {} database to {} and {}",
                        config.get("database.config.dataSource.databaseName"), migrationUser, appUser);
            }
        } catch (Exception e) {
            log.error("Failed to grant permissions", e);
            throw new RuntimeException("Failed to grant permissions", e);
        } finally {
            stingraySqlDatasource.close();
        }
    }

    private void migrateDatabase(ApplicationProperties config) {
        StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("flyway.migration.config"), "embedded", StingraySqlDatasourceFactory.class);
        StingrayFlywayMigrationHelper flywayMigrationHelper = StingrayFlywayMigrationHelper.forMigration(
                "migrations", stingraySqlDatasource,
                config.get("database.config.dataSource.user")
        );
        flywayMigrationHelper.upgradeDatabase();
        stingraySqlDatasource.close();
    }
}