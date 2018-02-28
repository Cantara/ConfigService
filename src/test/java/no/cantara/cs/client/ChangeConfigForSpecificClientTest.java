package no.cantara.cs.client;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.Client;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.BaseSystemTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class ChangeConfigForSpecificClientTest extends BaseSystemTest {

    private ConfigServiceClient configServiceClient;
    private Application application;
    private ConfigServiceAdminClient configServiceAdminClient;

    private ClientConfig currentClientConfig;

    @BeforeClass
    public void setup() throws Exception {
        configServiceClient = getConfigServiceClient();

        configServiceAdminClient = getConfigServiceAdminClient();
        application = configServiceAdminClient.registerApplication(getClass().getSimpleName());
        configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("default-config", application));

        // Register client
        currentClientConfig = configServiceClient.registerClient(new ClientRegistrationRequest(application.artifactId));
        configServiceClient.saveApplicationState(currentClientConfig);
    }

    @Test
    public void testChangeConfigForSingleClient() throws IOException {
        // Create a new config
        String newConfigIdentifier = "for-single-client";
        ApplicationConfig newConfig = configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto(newConfigIdentifier, application));

        // Register that client should use new config
        Client client = configServiceAdminClient.getClient(this.currentClientConfig.clientId);
        client.applicationConfigId = newConfig.getId();
        Client putClientResponse = configServiceAdminClient.putClient(client);

        assertNotNull(putClientResponse);
        assertEquals(putClientResponse.applicationConfigId, newConfig.getId());
        assertEquals(putClientResponse.clientId, client.clientId);
    }

    @Test(dependsOnMethods = "testChangeConfigForSingleClient")
    public void testCheckForUpdateWithNewClientSpecificConfig() throws Exception {
        String previousLastChanged = configServiceClient.getApplicationState().getProperty("lastChanged");

        // CheckForUpdate should return new config
        ClientConfig checkForUpdateResponse = configServiceClient.checkForUpdate(this.currentClientConfig.clientId, new CheckForUpdateRequest(previousLastChanged));
        assertNotNull(checkForUpdateResponse);
        assertNotEquals(checkForUpdateResponse.config.getId(), currentClientConfig.config.getId());
    }

}
