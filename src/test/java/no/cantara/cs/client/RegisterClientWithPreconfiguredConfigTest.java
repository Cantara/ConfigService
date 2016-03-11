package no.cantara.cs.client;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.dto.Config;
import no.cantara.cs.testsupport.ConfigBuilder;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class RegisterClientWithPreconfiguredConfigTest {

    private ConfigServiceClient configServiceClient;
    private Application application;
    private ConfigServiceAdminClient configServiceAdminClient;

    private TestServer testServer;
    private Config clientSpecificConfig;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        application = configServiceAdminClient.registerApplication("RegisterClientWithPreconfiguredConfigTest");
        configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto("default-config", application));
        clientSpecificConfig = configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto("client-specific-config", application));
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    public void testRegisterClientWithPreconfiguredConfig() throws Exception {
        String clientId = "client-1-id";
        Config config = configServiceAdminClient.updateClientConfig(clientId, clientSpecificConfig.getId());

        ClientRegistrationRequest registration = new ClientRegistrationRequest(application.artifactId);
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client-1-name";
        registration.clientId = clientId;

        ClientConfig clientConfig = configServiceClient.registerClient(registration);
        assertNotNull(clientConfig);
        assertEquals(clientConfig.config.getId(), clientSpecificConfig.getId());
    }

}
