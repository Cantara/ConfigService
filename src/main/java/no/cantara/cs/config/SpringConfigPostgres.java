package no.cantara.cs.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.cantara.cs.persistence.PersistedConfigRepoPostgres;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@ComponentScan(basePackages = "no.cantara.cs", excludeFilters = {
        /* Exclude the Embedded Spring configuration. */
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                value = {SpringConfigEmbedded.class})
})
public class SpringConfigPostgres {
    private static final Logger log = LoggerFactory.getLogger(SpringConfigPostgres.class);

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

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        String url = ConstrettoConfig.getString("postgres.url");
        String username = ConstrettoConfig.getString("postgres.username");

        log.info("Using db config: url={}, username={}", url,
                username);

        final HikariConfig config = new HikariConfig();
        config.setDriverClassName(ConstrettoConfig.getString("postgres.driver.class.name"));
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(ConstrettoConfig.getString("postgres.password"));
        config.setMaximumPoolSize(ConstrettoConfig.getInt("postgres.max.connections"));
        config.setIdleTimeout(ConstrettoConfig.getInt("postgres.connection.idleTimeout"));
        config.setMinimumIdle(ConstrettoConfig.getInt("postgres.minimum.idle.connections"));
        return new HikariDataSource(config);
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
        Flyway flyway = Flyway.configure().baselineOnMigrate(true).locations("migrations").dataSource(dataSource).load();
//        flyway.setBaselineOnMigrate(true);
//        flyway.setLocations("migrations");
        return flyway;
    }
}

