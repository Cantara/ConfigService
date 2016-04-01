package no.cantara.cs.admin;

import com.jayway.restassured.http.ContentType;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientEnvironment;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class ClientEnvTest {

    private Application application;

    private TestServer testServer;
    private ClientConfig clientConfig;

    @BeforeClass
    public void setup() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();

        ConfigServiceAdminClient configServiceAdminClient = testServer.getAdminClient();
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
    public void testClientEnvAfterRegisterClient() throws Exception {
        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(application.artifactId);
        registrationRequest.envInfo = new HashMap<>();
        registrationRequest.envInfo.put("var1", "value1");
        clientConfig = testServer.getConfigServiceClient().registerClient(registrationRequest);

        ClientEnvironment clientEnvironment = testServer.getAdminClient().getClientEnvironment(clientConfig.clientId);
        assertNotNull(clientEnvironment);
        assertNotNull(clientEnvironment.envInfo);
        assertEquals(clientEnvironment.envInfo.get("var1"), "value1");
        assertNotNull(clientEnvironment.timestamp);
    }

    @Test(dependsOnMethods = "testClientEnvAfterRegisterClient")
    public void testClientEnvAfterCheckForUpdate() throws IOException {
        HashMap<String, String> envInfo = new HashMap<>();
        envInfo.put("var2", "value2");
        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest("force-new-config", envInfo);
        testServer.getConfigServiceClient().checkForUpdate(this.clientConfig.clientId, checkForUpdateRequest);

        ClientEnvironment clientEnvironment = testServer.getAdminClient().getClientEnvironment(clientConfig.clientId);
        assertNotNull(clientEnvironment);
        assertNotNull(clientEnvironment.envInfo);
        assertEquals(clientEnvironment.envInfo.get("var2"), "value2");
        assertNotNull(clientEnvironment.timestamp);
    }

    @Test
    public void testClientEnvForNonExistingClientIdShouldGiveNotFound() throws Exception {
        given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().everything()
                .when()
                .get(ClientResource.CLIENT_PATH + "/{clientId}/env", "non-exising-client-id");
    }

}
