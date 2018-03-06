package no.cantara.cs.config;

import no.cantara.cs.persistence.PersistedConfigRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@ComponentScan(basePackages = "no.cantara.cs", excludeFilters = {
        /* Exclude the Postgres Spring configuration */
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                value = {SpringConfigPostgres.class})
})
public class SpringConfigMapDb {
    private static final Logger log = LoggerFactory.getLogger(SpringConfigMapDb.class);

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PersistedConfigRepo repo(@Value("${mapdb.path}") String mapDbPath) {
        log.info("mapdb.path: {}", mapDbPath);
        return new PersistedConfigRepo(mapDbPath);
    }
}
