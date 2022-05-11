package no.cantara.cs.embedded;

import static org.testng.Assert.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.stingray.sql.StingrayFlywayMigrationHelper;
import no.cantara.stingray.sql.StingraySqlDatasource;
import no.cantara.stingray.sql.StingraySqlDatasourceFactory;
import org.testng.annotations.Test;

public class EmbeddedPostgresStingraySqlDatasourceTest {

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

  @Test
  public void thatEmbeddedDataSourceWorks() throws SQLException {
    ApplicationProperties config = ApplicationProperties.builder()
        .classpathPropertiesFile("embedded.properties")
        .build();
    createDatabase(config);
    migrateDatabase(config);
    StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("database.config"), "embedded", StingraySqlDatasourceFactory.class);
    try {
      DataSource dataSource = stingraySqlDatasource.getDataSource();
      String generatedApplicationId = UUID.randomUUID().toString();
      try (Connection connection = dataSource.getConnection()) {
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO Applications(id, artifact_id) VALUES(?,?)")) {
          ps.setString(1, generatedApplicationId);
          ps.setString(2, "myArtifact");
          int n = ps.executeUpdate();
          assertEquals(1, n);
        }
        try (PreparedStatement ps = connection.prepareStatement("SELECT id, artifact_id FROM Applications WHERE id = ?")) {
          ps.setString(1, generatedApplicationId);
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              String applicationId = rs.getString(1);
              String artifactId = rs.getString(2);
              assertEquals(generatedApplicationId, applicationId);
              assertEquals("myArtifact", artifactId);

            }
          }
        }
      }
    } finally {
      stingraySqlDatasource.close();
    }
  }


}
