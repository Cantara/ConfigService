package no.cantara.cs.clientstatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.response.Response;
import no.cantara.cs.client.ClientStatus;
import no.cantara.cs.config.ApplicationResource;
import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.ConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
import no.cantara.cs.testsupport.dto.ApplicationStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class ClientStatusTest {

    private ConfigServiceClient configServiceClient;
    private Application application;

    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        ConfigServiceAdminClient configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        application = configServiceAdminClient.registerApplication("RegisterClientTest");
        configServiceAdminClient.registerConfig(application, ConfigBuilder.createConfigDto("arbitrary-config", application));
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    public void testStatusShouldBeAvailableAfterRegisterClient() throws Exception {
        ClientRegistrationRequest clientRegistrationRequest = new ClientRegistrationRequest(application.artifactId);
        clientRegistrationRequest.clientName = "client-name";
        clientRegistrationRequest.clientName = "tags";
        ClientConfig clientConfig = configServiceClient.registerClient(clientRegistrationRequest);
        ApplicationStatus applicationStatus = testServer.getAdminClient().queryApplicationStatus(application.artifactId);

        assertEquals(applicationStatus.numberOfRegisteredClients, Integer.valueOf(1));
        assertEquals(applicationStatus.seenInTheLastHourCount, Integer.valueOf(1));
        assertNotNull(applicationStatus.seenInTheLastHour);
        assertEquals(applicationStatus.seenInTheLastHour.size(), 1);
        assertNotNull(applicationStatus.allClientsSnapshot);
        ClientStatus clientStatus = applicationStatus.allClientsSnapshot.get(clientConfig.clientId);
        assertNotNull(clientStatus);
        assertEquals(clientStatus.artifactId, application.artifactId);
        assertEquals(clientStatus.clientName, clientRegistrationRequest.clientName);
        assertEquals(clientStatus.tags, clientRegistrationRequest.tags);
    }
}
