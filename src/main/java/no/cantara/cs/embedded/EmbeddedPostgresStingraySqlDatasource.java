package no.cantara.cs.embedded;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import no.cantara.stingray.sql.StingraySqlDatasource;

public class EmbeddedPostgresStingraySqlDatasource implements StingraySqlDatasource {

    private static class EmbeddedPostgresSingletonHolder {
        private static final EmbeddedPostgres embeddedPostgres;

        static {
            try {
                embeddedPostgres = EmbeddedPostgres.start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    final EmbeddedPostgres embeddedPostgres;
    final String dbName;
    final String userName;
    final String password;

    public EmbeddedPostgresStingraySqlDatasource(String dbName, String userName, String password) {
        this.dbName = dbName;
        this.userName = userName;
        this.password = password;
        this.embeddedPostgres = EmbeddedPostgresSingletonHolder.embeddedPostgres;
    }

    public EmbeddedPostgres getEmbeddedPostgres() {
        return embeddedPostgres;
    }

    public DataSource getDataSource() {
        Map<String, String> properties = new LinkedHashMap<>();
        if (password != null && !password.trim().isEmpty()) {
            properties.put("password", this.password);
        }
        return embeddedPostgres.getDatabase(userName, dbName, properties);
    }

    @Override
    public String info() {
        return "embedded-postgres-to-be-used-only-for-testing";
    }

    @Override
    public void close() {
        /*
         * Do not close anything, as the underlying embedded-postgres might be re-used in another test after this close.
         */
    }

    public void clearTables() {
        DataSource dataSource = getDataSource();
        try (Connection connection = dataSource.getConnection()) {
            String sql = loadResourceContentAsString();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.executeBatch();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String loadResourceContentAsString() {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = getClass().getResourceAsStream("truncate_all_tables.sql")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }
}
