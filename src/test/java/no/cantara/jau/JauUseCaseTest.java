package no.cantara.jau;

import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.*;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionConfig;
import no.cantara.jau.testsupport.ConfigServiceAdminClient;
import no.cantara.jau.testsupport.ServiceConfigBuilder;
import no.cantara.jau.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.NoContentException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.*;

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

        configServiceAdminClient.registerServiceConfig(application, ServiceConfigBuilder.createServiceConfigDto("JauUseCaseTest", application));
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
        configServiceClient.checkForUpdate(currentClientConfig.clientId, new CheckForUpdateRequest(currentClientConfig.serviceConfig.getLastChanged()));
    }

    @Test(dependsOnMethods = "testCheckForUpdateWithUpToDateClientConfig")
    public void testChangeServiceConfigForSingleClient() throws IOException {
        // Create a new service config
        String newServiceConfigIdentifier = "for-single-client";
        ServiceConfig newServiceConfig = configServiceAdminClient.registerServiceConfig(application, ServiceConfigBuilder.createServiceConfigDto(newServiceConfigIdentifier, application));

        // Register that client should use new service config
        ClientConfig updatedClientConfig = new ClientConfig(this.currentClientConfig.clientId, newServiceConfig);
        ServiceConfig updateClientConfigResponse = configServiceAdminClient.updateClientConfig(this.currentClientConfig.clientId, updatedClientConfig);

        assertNotNull(updateClientConfigResponse);
        assertEquals(updateClientConfigResponse.getId(), newServiceConfig.getId());
        assertTrue(updateClientConfigResponse.getName().contains(newServiceConfigIdentifier));
    }

    @Test(dependsOnMethods = "testChangeServiceConfigForSingleClient")
    public void testCheckForUpdateWithNewClientSpecificServiceConfig() throws Exception {
        String previousLastChanged = configServiceClient.getApplicationState().getProperty("lastChanged");

        // CheckForUpdate should return new service config
        ClientConfig checkForUpdateResponse = configServiceClient.checkForUpdate(this.currentClientConfig.clientId, new CheckForUpdateRequest(previousLastChanged));
        assertNotNull(checkForUpdateResponse);
        assertNotEquals(checkForUpdateResponse.serviceConfig.getId(), currentClientConfig.serviceConfig.getId());

        // Save state and verify lastChanged is updated
        configServiceClient.saveApplicationState(checkForUpdateResponse);
        assertEquals(configServiceClient.getApplicationState().getProperty("lastChanged"), checkForUpdateResponse.serviceConfig.getLastChanged());

        currentClientConfig = checkForUpdateResponse;
    }

    // Not supported yet
    @Test(enabled = false, dependsOnMethods = "testCheckForUpdateWithNewClientSpecificServiceConfig")
    public void testCheckForUpdateWhenCurrentServiceConfigHasBeenChanged() throws Exception {
        // Update current serviceconfig by setting lastChanged
        ServiceConfig updatedServiceConfig = ServiceConfigBuilder.createServiceConfigDto("UpdatedServiceConfig", application);
        updatedServiceConfig.setId(currentClientConfig.serviceConfig.getId());

        ServiceConfig updateServiceConfigResponse = configServiceAdminClient.updateServiceConfig(application.id, updatedServiceConfig);
        assertEquals(updateServiceConfigResponse.getId(), updatedServiceConfig.getId());
        assertNotEquals(updateServiceConfigResponse.getLastChanged(), currentClientConfig.serviceConfig.getLastChanged());

        // CheckForUpdate should return updated config
        ClientConfig checkForUpdateResponse = configServiceClient.checkForUpdate(this.currentClientConfig.clientId, new CheckForUpdateRequest(updateServiceConfigResponse.getLastChanged()));
        assertNotNull(checkForUpdateResponse);

        // Save state and verify lastChanged is updated
        configServiceClient.saveApplicationState(checkForUpdateResponse);
        assertEquals(configServiceClient.getApplicationState().getProperty("lastChanged"), updateServiceConfigResponse.getLastChanged());

        currentClientConfig = checkForUpdateResponse;
    }

    // Not supported yet
    @Test(enabled = false, dependsOnMethods = "testCheckForUpdateWithNewClientSpecificServiceConfig")
    public void testCheckForUpdateWithNewDefaultServiceConfig() throws Exception {
        ServiceConfig newDefaultServiceConfig = configServiceAdminClient.registerServiceConfig(application, ServiceConfigBuilder.createServiceConfigDto("NewDefaultServiceConfigTest", application));

        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest(currentClientConfig.serviceConfig.getLastChanged());
        ClientConfig checkForUpdateResponse = configServiceClient.checkForUpdate(currentClientConfig.clientId, checkForUpdateRequest);

        assertNotNull(checkForUpdateResponse);
        assertEquals(checkForUpdateResponse.serviceConfig.getId(), newDefaultServiceConfig.getId());

        // Save state and verify lastChanged is updated
        configServiceClient.saveApplicationState(checkForUpdateResponse);
        assertEquals(configServiceClient.getApplicationState().getProperty("lastChanged"), newDefaultServiceConfig.getLastChanged());

        currentClientConfig = checkForUpdateResponse;
    }

}
