package no.cantara.cs.testsupport;

import com.jayway.restassured.RestAssured;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import no.cantara.cs.Main;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.config.ConstrettoConfig;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public class TestServerPostgres implements TestServer {

    private Main main;
    private String url;
    private Class testClass;

    public TestServerPostgres(Class testClass) {
        this.testClass = testClass;
    }

    public void cleanAllData() throws Exception {
        final HikariConfig config = new HikariConfig();
        config.setDriverClassName(ConstrettoConfig.getString("postgres.driver.class.name"));
        config.setJdbcUrl(ConstrettoConfig.getString("postgres.url"));
        config.setUsername(ConstrettoConfig.getString("postgres.username"));
        config.setPassword(ConstrettoConfig.getString("postgres.password"));
        config.setMaximumPoolSize(1);
        final HikariDataSource dataSource = new HikariDataSource(config);
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
        dataSource.close();
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


    public void start() throws InterruptedException {
        new Thread(() -> {
            main = new Main("postgres");
            main.start();
        }).start();
        do {
            Thread.sleep(10);
        } while (main == null || !main.isStarted());
        RestAssured.port = main.getPort();

        RestAssured.basePath = Main.CONTEXT_PATH;
        url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH + ClientResource.CLIENT_PATH;
    }

    public void stop() {
        main.stop();
    }

    public ConfigServiceClient getConfigServiceClient() {
        return new ConfigServiceClient(url, USERNAME, PASSWORD).withApplicationStateFilename("target/" + testClass.getSimpleName() + ".properties");
    }

    public ConfigServiceAdminClient getAdminClient() {
        return new ConfigServiceAdminClient("http://localhost:" + main.getPort() + Main.CONTEXT_PATH, TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD);
    }
}
