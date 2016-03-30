package no.cantara.cs.admin;

import com.jayway.restassured.http.ContentType;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.dto.*;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ClientStatusTest {

    private Application application;

    private TestServer testServer;
    private ClientConfig clientConfig;

    @BeforeClass
    public void setup() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();

        ConfigServiceAdminClient configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        application = configServiceAdminClient.registerApplication("ClientStatusTest-ArtifactId");
        configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("arbitrary-config", application));
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }

    @Test
    public void testClientStatusAfterRegisterClient() throws Exception {
        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(application.artifactId);
        registrationRequest.clientName = "client-name";
        registrationRequest.tags = "tags";
        clientConfig = testServer.getConfigServiceClient().registerClient(registrationRequest);

        ClientStatus clientStatus = testServer.getAdminClient().getClientStatus(clientConfig.clientId);
        assertEquals(clientStatus.client.clientId, clientConfig.clientId);
        assertEquals(clientStatus.client.applicationConfigId, clientConfig.config.getId());
        assertEquals(clientStatus.client.autoUpgrade, true); // Default should be true

        ClientHeartbeatData latestClientHeartbeatData = clientStatus.latestClientHeartbeatData;
        assertNotNull(latestClientHeartbeatData);
        assertEquals(latestClientHeartbeatData.applicationConfigId, clientConfig.config.getId());
        assertEquals(latestClientHeartbeatData.artifactId, application.artifactId);
        assertEquals(latestClientHeartbeatData.clientName, registrationRequest.clientName);
        assertEquals(latestClientHeartbeatData.tags, registrationRequest.tags);
    }

    @Test(dependsOnMethods = "testClientStatusAfterRegisterClient")
    public void testClientStatusAfterCheckForUpdate() throws IOException {
        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest("force-new-config", new HashMap<>(), "checkForUpdateTags", "checkForUpdateName");
        ClientConfig checkForUpdateResponse = testServer.getConfigServiceClient().checkForUpdate(this.clientConfig.clientId, checkForUpdateRequest);

        ClientStatus clientStatus = testServer.getAdminClient().getClientStatus(clientConfig.clientId);
        assertEquals(clientStatus.client.clientId, checkForUpdateResponse.clientId);
        assertEquals(clientStatus.client.applicationConfigId, checkForUpdateResponse.config.getId());

        ClientHeartbeatData latestClientHeartbeatData = clientStatus.latestClientHeartbeatData;
        assertNotNull(latestClientHeartbeatData);
        assertEquals(latestClientHeartbeatData.applicationConfigId, checkForUpdateResponse.config.getId());
        assertEquals(latestClientHeartbeatData.artifactId, application.artifactId);
        assertEquals(latestClientHeartbeatData.clientName, checkForUpdateRequest.clientName);
        assertEquals(latestClientHeartbeatData.tags, checkForUpdateRequest.tags);
    }

    @Test
    public void testClientStatusForNonExistingClientIdShouldGiveNotFound() throws Exception {
        given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().everything()
                .when()
                .get(ClientResource.CLIENT_PATH + "/{clientId}/status", "non-exising-client-id");
    }

}
