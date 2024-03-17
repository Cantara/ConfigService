package no.cantara.cs.testsupport;

import io.restassured.RestAssured;
import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.cs.Main;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.client.ConfigServiceClient;
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
    migrateDatabase(config);
    StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("database.config"), "embedded", StingraySqlDatasourceFactory.class);
    try{
      DataSource dataSource = stingraySqlDatasource.getDataSource();
      try(Connection connection = dataSource.getConnection()){
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
    }finally {
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

  private void migrateDatabase(ApplicationProperties config) {
    StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("flyway.migration.config"), "embedded", StingraySqlDatasourceFactory.class);
    StingrayFlywayMigrationHelper flywayMigrationHelper = StingrayFlywayMigrationHelper.forMigration(
        "db/migration", stingraySqlDatasource,
        config.get("database.config.dataSource.user")
    );
    flywayMigrationHelper.upgradeDatabase();
    stingraySqlDatasource.close();
  }
}
