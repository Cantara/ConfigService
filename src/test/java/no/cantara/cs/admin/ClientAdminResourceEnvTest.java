package no.cantara.cs.admin;

import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.dto.*;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.BaseSystemTest;
import no.cantara.cs.testsupport.TestServer;
import no.cantara.cs.testsupport.TestServerPostgres;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Asbjørn Willersrud
 */
public class ClientAdminResourceEnvTest extends BaseSystemTest {
    private Application application;

    private ClientConfig clientConfig;

    @BeforeClass
    public void setup() throws Exception {
        ConfigServiceAdminClient configServiceAdminClient = getTestServer().getAdminClient();
        application = configServiceAdminClient.registerApplication("ClientStatusTest-ArtifactId");
        configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("arbitrary-config", application));
    }

    @Test
    public void testClientEnvAfterRegisterClient() throws Exception {
        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(application.artifactId);
        registrationRequest.envInfo = new HashMap<>();
        registrationRequest.envInfo.put("var1", "value1");
        clientConfig = getTestServer().getConfigServiceClient().registerClient(registrationRequest);

        ClientEnvironment clientEnvironment = getTestServer().getAdminClient().getClientEnvironment(clientConfig.clientId);
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
        getTestServer().getConfigServiceClient().checkForUpdate(this.clientConfig.clientId, checkForUpdateRequest);

        ClientEnvironment clientEnvironment = getTestServer().getAdminClient().getClientEnvironment(clientConfig.clientId);
        assertNotNull(clientEnvironment);
        assertNotNull(clientEnvironment.envInfo);
        assertEquals(clientEnvironment.envInfo.get("var2"), "value2");
        assertNotNull(clientEnvironment.timestamp);
    }

    @Test
    public void testClientEnvForNonExistingClientIdShouldGiveNotFound() throws Exception {
        given()
                .auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .contentType(MediaType.APPLICATION_JSON)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().everything()
                .when()
                .get(ClientResource.CLIENT_PATH + "/{clientId}/env", "non-exising-client-id");
    }

}
