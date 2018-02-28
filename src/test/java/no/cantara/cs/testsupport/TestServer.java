package no.cantara.cs.testsupport;

import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.client.ConfigServiceClient;

public interface TestServer {
    String USERNAME = "read";
    String PASSWORD = "baretillesing";
    String ADMIN_USERNAME = "admin";
    String ADMIN_PASSWORD = "configservice";

    void cleanAllData() throws Exception;

    void start() throws InterruptedException;

    void stop();

    ConfigServiceClient getConfigServiceClient();

    ConfigServiceAdminClient getAdminClient();
}
