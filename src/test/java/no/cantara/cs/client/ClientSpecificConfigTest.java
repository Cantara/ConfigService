package no.cantara.cs.client;

import no.cantara.cs.dto.*;
import no.cantara.cs.testsupport.ConfigBuilder;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.TestServer;
import no.cantara.cs.testsupport.dto.ApplicationStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

public class ClientSpecificConfigTest {

    private ConfigServiceClient configServiceClient;
    private Application application;
    private ConfigServiceAdminClient configServiceAdminClient;

    private TestServer testServer;
    private ClientConfig currentClientConfig;
    private ClientConfig preconfiguredClientConfig;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        application = configServiceAdminClient.registerApplication(getClass().getSimpleName());
        configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto("default-config", application));

        // Register client
        currentClientConfig = configServiceClient.registerClient(new ClientRegistrationRequest(application.artifactId));
        configServiceClient.saveApplicationState(currentClientConfig);
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
        configServiceClient.cleanApplicationState();
    }

    @Test
    public void testChangeConfigForSingleClient() throws IOException {
        // Create a new config
        String newConfigIdentifier = "for-single-client";
        Config newConfig = configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto(newConfigIdentifier, application));

        // Register that client should use new config
        Config updateClientConfigResponse = configServiceAdminClient.updateClientConfig(this.currentClientConfig.clientId, newConfig.getId());

        assertNotNull(updateClientConfigResponse);
        assertEquals(updateClientConfigResponse.getId(), newConfig.getId());
        assertTrue(updateClientConfigResponse.getName().contains(newConfigIdentifier));
    }

    @Test(dependsOnMethods = "testChangeConfigForSingleClient")
    public void testCheckForUpdateWithNewClientSpecificConfig() throws Exception {
        String previousLastChanged = configServiceClient.getApplicationState().getProperty("lastChanged");

        // CheckForUpdate should return new config
        ClientConfig checkForUpdateResponse = configServiceClient.checkForUpdate(this.currentClientConfig.clientId, new CheckForUpdateRequest(previousLastChanged));
        assertNotNull(checkForUpdateResponse);
        assertNotEquals(checkForUpdateResponse.config.getId(), currentClientConfig.config.getId());

        // Save state and verify lastChanged is updated
        configServiceClient.saveApplicationState(checkForUpdateResponse);
        assertEquals(configServiceClient.getApplicationState().getProperty("lastChanged"), checkForUpdateResponse.config.getLastChanged());

        currentClientConfig = checkForUpdateResponse;
    }

    @Test(dependsOnMethods = "testCheckForUpdateWithNewClientSpecificConfig")
    public void testRegisterClientWithPreconfiguredConfig() throws Exception {
        Config config = configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto("pre-registered-config", application));

        String clientId = "client-with-preconfigured-config-id";
        Config updateClientConfigResponse = configServiceAdminClient.updateClientConfig(clientId, config.getId());
        assertEquals(updateClientConfigResponse.getId(), config.getId());

        ClientRegistrationRequest registration = new ClientRegistrationRequest(application.artifactId);
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client-with-preconfigured-config-name";
        registration.clientId = clientId;

        preconfiguredClientConfig = configServiceClient.registerClient(registration);
        assertNotNull(preconfiguredClientConfig);
        assertEquals(preconfiguredClientConfig.config.getId(), config.getId());
    }

    @Test(dependsOnMethods = "testRegisterClientWithPreconfiguredConfig")
    public void testGetApplicationStatusWithClientSpecificConfigs() throws IOException {
        ApplicationStatus applicationStatus = configServiceAdminClient.queryApplicationStatus(application.artifactId);
        assertNotNull(applicationStatus.allClientsSnapshot.get(preconfiguredClientConfig.clientId));
        assertNotNull(applicationStatus.allClientsSnapshot.get(currentClientConfig.clientId));
    }

}
