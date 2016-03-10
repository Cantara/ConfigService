package no.cantara.cs.clientstatus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.response.Response;
import no.cantara.cs.config.ApplicationResource;
import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.ConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
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
        ClientConfig clientConfig = configServiceClient.registerClient(new ClientRegistrationRequest(application.artifactId));

        Response response = given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .get(ApplicationResource.APPLICATION_PATH + "/" + application.artifactId + "/status");
        assertTrue(response.body().asString().contains(clientConfig.clientId), "Was: " + response.body().asString() + "");

        Map statusValues = new ObjectMapper().readValue(response.body().asString(), Map.class);

        assertEquals(statusValues.get("numberOfRegisteredClients"), 1);
        assertEquals(statusValues.get("seenInTheLastHourCount"), 1);
        assertNotNull(statusValues.get("seenInTheLastHour"));
        assertEquals(((List)statusValues.get("seenInTheLastHour")).size(), 1);
        assertNotNull(statusValues.get("allClientsSnapshot"));
        assertNotNull(((Map)statusValues.get("allClientsSnapshot")).get(clientConfig.clientId));
    }
}
