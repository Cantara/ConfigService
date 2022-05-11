package no.cantara.cs.embedded;

import no.cantara.config.ApplicationProperties;
import no.cantara.stingray.sql.StingraySqlDatasourceFactory;

public class EmbeddedPostgresStingraySqlDatasourceFactory implements StingraySqlDatasourceFactory {
    @Override
    public Class<?> providerClass() {
        return EmbeddedPostgresStingraySqlDatasource.class;
    }

    @Override
    public String alias() {
        return "embedded";
    }

    @Override
    public EmbeddedPostgresStingraySqlDatasource create(ApplicationProperties config) {
        String databaseName = config.get("dataSource.databaseName");
        String user = config.get("dataSource.user");
        String password = config.get("dataSource.password");
        return new EmbeddedPostgresStingraySqlDatasource(databaseName, user, password);
    }
}
