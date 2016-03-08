package no.cantara.jau.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.response.Response;
import no.cantara.jau.serviceconfig.ApplicationResource;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.Application;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
import no.cantara.jau.testsupport.ConfigServiceAdminClient;
import no.cantara.jau.testsupport.ServiceConfigBuilder;
import no.cantara.jau.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class ApplicationResourceTest {

    private ConfigServiceClient configServiceClient;
    private Application application;
    private ConfigServiceAdminClient configServiceAdminClient;

    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        application = configServiceAdminClient.registerApplication("RegisterClientTest");
        configServiceAdminClient.registerServiceConfig(application, ServiceConfigBuilder.createServiceConfigDto("arbitrary-config", application));
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

        String path = ApplicationResource.APPLICATION_PATH + "/" + application.artifactId + "/status";

        Response response = given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .get(path);
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
