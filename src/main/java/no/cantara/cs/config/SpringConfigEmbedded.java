package no.cantara.cs.config;

import no.cantara.config.ApplicationProperties;
import no.cantara.config.ProviderLoader;
import no.cantara.cs.persistence.PersistedConfigRepoPostgres;
import no.cantara.stingray.sql.StingraySqlDatasource;
import no.cantara.stingray.sql.StingraySqlDatasourceFactory;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@ComponentScan(basePackages = "no.cantara.cs", excludeFilters = {
    /* Exclude the Postgres Spring configuration. */
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        value = {SpringConfigPostgres.class})
})
public class SpringConfigEmbedded {
  private static final Logger log = LoggerFactory.getLogger(SpringConfigEmbedded.class);

  /**
   * Spring properties
   */
  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public PersistedConfigRepoPostgres repo(JdbcTemplate jdbcTemplate) {
    return new PersistedConfigRepoPostgres(jdbcTemplate);
  }

  @Bean
  public DataSource dataSource() {
    ApplicationProperties config = ApplicationProperties.builder()
        .classpathPropertiesFile("embedded.properties")
        .build();
    StingraySqlDatasource stingraySqlDatasource = ProviderLoader.configure(config.subTree("database.config"), "embedded", StingraySqlDatasourceFactory.class);
    return stingraySqlDatasource.getDataSource();
  }

  @Bean
  PlatformTransactionManager dataSourceTransactionManager(final DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }

  @Bean
  JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }


    @Bean(initMethod = "migrate")
    Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .baselineOnMigrate(true)
                .locations("migrations")
                .dataSource(dataSource)
                .load();
        return flyway;
    }
}
