package no.cantara.cs.client;

import no.cantara.cs.application.ApplicationStatus;
import no.cantara.cs.dto.*;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class RegisterClientWithPreconfiguredConfigTest {

    private ConfigServiceClient configServiceClient;
    private ConfigServiceAdminClient configServiceAdminClient;

    private TestServer testServer;
    private ClientConfig preconfiguredClientConfig;
    private String artifactId;
    private Application application;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        artifactId = getClass().getSimpleName();
        configServiceAdminClient.registerApplication(artifactId);
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    public void findApplicationIdByArtifactId() throws IOException {
        List<Application> allApplications = configServiceAdminClient.getAllApplications();
        Optional<Application> optionalApplication = allApplications.stream().filter(a -> a.artifactId.equals(artifactId)).findAny();
        assertTrue(optionalApplication.isPresent());
        application = optionalApplication.get();
    }

    @Test(dependsOnMethods = "findApplicationIdByArtifactId")
    public void testRegisterClientWithPreconfiguredConfig() throws Exception {
        ApplicationConfig config = configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("pre-registered-config", application));

        String clientId = "client-with-preconfigured-config-id";
        Client updateClientResponse = configServiceAdminClient.putClient(new Client(clientId, config.getId(), true));
        assertEquals(updateClientResponse.applicationConfigId, config.getId());

        ClientRegistrationRequest registration = new ClientRegistrationRequest(application.artifactId);
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client-with-preconfigured-config-name";
        registration.clientId = clientId;

        preconfiguredClientConfig = configServiceClient.registerClient(registration);
        assertNotNull(preconfiguredClientConfig);
        assertEquals(preconfiguredClientConfig.config.getId(), config.getId());
    }

    @Test(dependsOnMethods = "testRegisterClientWithPreconfiguredConfig")
    public void testGetApplicationStatus() throws IOException {
        ApplicationStatus applicationStatus = configServiceAdminClient.getApplicationStatus(application.artifactId);
        assertNotNull(applicationStatus.allClientHeartbeatData.get(preconfiguredClientConfig.clientId));
    }

}
