package no.cantara.cs.admin;

import no.cantara.cs.application.ApplicationStatus;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ApplicationStatusTest {

    private Application application;

    private TestServer testServer;
    private ClientConfig client1;
    private ClientConfig client2;

    @BeforeClass
    public void setup() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();

        ConfigServiceAdminClient configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        application = configServiceAdminClient.registerApplication(getClass().getSimpleName());
        configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("arbitrary-config", application));

        // Only register client 1
        client1 = testServer.getConfigServiceClient().registerClient(new ClientRegistrationRequest(application.artifactId, "client-1-name"));

        // Register and update client 2
        ClientConfig registerClientResponse = testServer.getConfigServiceClient().registerClient(new ClientRegistrationRequest(application.artifactId, "client-2-name"));
        client2 = testServer.getConfigServiceClient().checkForUpdate(registerClientResponse.clientId, new CheckForUpdateRequest("force-updated-config"));
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    public void testApplicationStatus() throws Exception {
        ApplicationStatus applicationStatus = testServer.getAdminClient().getApplicationStatus(application.artifactId);

        assertEquals(applicationStatus.numberOfRegisteredClients, Integer.valueOf(2));
        assertEquals(applicationStatus.seenInTheLastHourCount, Integer.valueOf(2));
        assertNotNull(applicationStatus.seenInTheLastHour);
        assertEquals(applicationStatus.seenInTheLastHour.size(), 2);
        assertTrue(applicationStatus.seenInTheLastHour.contains(client1.clientId));
        assertTrue(applicationStatus.seenInTheLastHour.contains(client2.clientId));
        assertNotNull(applicationStatus.allClientHeartbeatData);
        assertNotNull(applicationStatus.allClientHeartbeatData.get(client1.clientId));
        assertNotNull(applicationStatus.allClientHeartbeatData.get(client2.clientId));
    }

}
