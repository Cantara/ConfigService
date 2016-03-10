package no.cantara.cs.client;

import no.cantara.cs.dto.*;
import no.cantara.cs.dto.event.EventExtractionConfig;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.ConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.NoContentException;
import java.io.IOException;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Verify endpoints used by JAU client.
 */
public class JauUseCaseTest {
    private TestServer testServer;
    private ConfigServiceClient configServiceClient;
    private ConfigServiceAdminClient configServiceAdminClient;

    private Application application;
    private ClientConfig currentClientConfig;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();
        configServiceAdminClient = testServer.getAdminClient();

        application = configServiceAdminClient.registerApplication("jau-use-case-test");

        configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto("JauUseCaseTest", application));
    }

    @AfterClass
    public void tearDown() {
        testServer.stop();
        configServiceClient.cleanApplicationState();
    }

    @Test
    public void startupAndRegisterClient() throws IOException {
        currentClientConfig = configServiceClient.registerClient(new ClientRegistrationRequest(application.artifactId));

        configServiceClient.saveApplicationState(currentClientConfig);
    }

    @Test(dependsOnMethods = "startupAndRegisterClient")
    public void testGetExtractionConfigs() throws IOException {
        List<EventExtractionConfig> tags = configServiceClient.getEventExtractionConfigs();

        assertEquals(tags.size(), 1);
        assertEquals(tags.get(0).groupName, "jau");
        assertEquals(tags.get(0).tags.get(0).filePath, "logs/blabla.logg");
    }

    @Test(dependsOnMethods = "startupAndRegisterClient", expectedExceptions = NoContentException.class)
    public void testCheckForUpdateWithUpToDateClientConfig() throws Exception {
        configServiceClient.checkForUpdate(currentClientConfig.clientId, new CheckForUpdateRequest(currentClientConfig.config.getLastChanged()));
    }

    @Test(dependsOnMethods = "testCheckForUpdateWithUpToDateClientConfig")
    public void testChangeConfigForSingleClient() throws IOException {
        // Create a new config
        String newConfigIdentifier = "for-single-client";
        Config newConfig = configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto(newConfigIdentifier, application));

        // Register that client should use new config
        ClientConfig updatedClientConfig = new ClientConfig(this.currentClientConfig.clientId, newConfig);
        Config updateClientConfigResponse = configServiceAdminClient.updateClientConfig(this.currentClientConfig.clientId, updatedClientConfig);

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
    public void testCheckForUpdateWhenCurrentConfigHasBeenChanged() throws Exception {
        // Update current config by setting lastChanged
        Config updatedConfig = ConfigBuilder.createConfigDto("UpdatedConfig", application);
        updatedConfig.getConfigurationStores().iterator().next().properties.put("new-property", "new-value");
        updatedConfig.setId(currentClientConfig.config.getId());
        assertNotEquals(currentClientConfig.config.getLastChanged(), updatedConfig.getLastChanged());

        Config updateConfigResponse = configServiceAdminClient.updateConfig(application.id, updatedConfig);
        assertEquals(updateConfigResponse.getId(), currentClientConfig.config.getId());
        assertNotEquals(updateConfigResponse.getLastChanged(), currentClientConfig.config.getLastChanged());

        // CheckForUpdate should return updated config
        ClientConfig checkForUpdateResponse = configServiceClient.checkForUpdate(this.currentClientConfig.clientId, new CheckForUpdateRequest(currentClientConfig.config.getLastChanged()));
        assertNotNull(checkForUpdateResponse);
        assertEquals(checkForUpdateResponse.config.getConfigurationStores().iterator().next().properties.get("new-property"), "new-value");

        // Save state and verify lastChanged is updated
        configServiceClient.saveApplicationState(checkForUpdateResponse);
        assertEquals(configServiceClient.getApplicationState().getProperty("lastChanged"), updateConfigResponse.getLastChanged());

        currentClientConfig = checkForUpdateResponse;
    }

    // Not supported yet
    @Test(enabled = false, dependsOnMethods = "testCheckForUpdateWithNewClientSpecificConfig")
    public void testCheckForUpdateWithNewDefaultConfig() throws Exception {
        Config newDefaultConfig = configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto("NewDefaultConfigTest", application));

        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest(currentClientConfig.config.getLastChanged());
        ClientConfig checkForUpdateResponse = configServiceClient.checkForUpdate(currentClientConfig.clientId, checkForUpdateRequest);

        assertNotNull(checkForUpdateResponse);
        assertEquals(checkForUpdateResponse.config.getId(), newDefaultConfig.getId());

        // Save state and verify lastChanged is updated
        configServiceClient.saveApplicationState(checkForUpdateResponse);
        assertEquals(configServiceClient.getApplicationState().getProperty("lastChanged"), newDefaultConfig.getLastChanged());

        currentClientConfig = checkForUpdateResponse;
    }

}
