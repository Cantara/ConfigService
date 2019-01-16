package no.cantara.cs.admin;

import com.jayway.restassured.http.ContentType;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientHeartbeatData;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.dto.ClientStatus;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.BaseSystemTest;
import no.cantara.cs.testsupport.TestServer;
import no.cantara.cs.testsupport.TestServerPostgres;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Asbjørn Willersrud
 */
public class ClientAdminResourceStatusTest extends BaseSystemTest {
    private Application application;
    private ClientConfig clientConfig;

    @BeforeClass
    public void setup() throws Exception {
        ConfigServiceAdminClient configServiceAdminClient = getConfigServiceAdminClient();
        application = configServiceAdminClient.registerApplication("ClientStatusTest-ArtifactId");
        configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("arbitrary-config", application));
    }

    @Test
    public void testClientStatusAfterRegisterClient() throws Exception {
        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(application.artifactId);
        registrationRequest.clientName = "client-name";
        registrationRequest.tags = "tags";
        clientConfig = getConfigServiceClient().registerClient(registrationRequest);

        ClientStatus clientStatus = getConfigServiceAdminClient().getClientStatus(clientConfig.clientId);
        assertEquals(clientStatus.client.clientId, clientConfig.clientId);
        assertEquals(clientStatus.client.applicationConfigId, clientConfig.config.getId());
        assertEquals(clientStatus.client.autoUpgrade, true); // Default should be true

        ClientHeartbeatData latestClientHeartbeatData = clientStatus.latestClientHeartbeatData;
        assertNotNull(latestClientHeartbeatData);
        assertEquals(latestClientHeartbeatData.applicationConfigId, clientConfig.config.getId());
        assertEquals(latestClientHeartbeatData.artifactId, application.artifactId);
        
        //clientName is now changed to the pattern computername - internal_ip - wrapped os
        //assertEquals(latestClientHeartbeatData.clientName, registrationRequest.clientName);
        assertEquals(latestClientHeartbeatData.tags, registrationRequest.tags);
    }

    @Test(dependsOnMethods = "testClientStatusAfterRegisterClient")
    public void testClientStatusAfterCheckForUpdate() throws IOException {
        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest("force-new-config", new HashMap<>(), "checkForUpdateTags", "checkForUpdateName");
        ClientConfig checkForUpdateResponse = getConfigServiceClient().checkForUpdate(this.clientConfig.clientId, checkForUpdateRequest);

        ClientStatus clientStatus = getConfigServiceAdminClient().getClientStatus(clientConfig.clientId);
        assertEquals(clientStatus.client.clientId, checkForUpdateResponse.clientId);
        assertEquals(clientStatus.client.applicationConfigId, checkForUpdateResponse.config.getId());

        ClientHeartbeatData latestClientHeartbeatData = clientStatus.latestClientHeartbeatData;
        assertNotNull(latestClientHeartbeatData);
        assertEquals(latestClientHeartbeatData.applicationConfigId, checkForUpdateResponse.config.getId());
        assertEquals(latestClientHeartbeatData.artifactId, application.artifactId);
        
        //clientName is now changed to the pattern computername - internal_ip - wrapped os
        //assertEquals(latestClientHeartbeatData.clientName, checkForUpdateRequest.clientName);
        assertEquals(latestClientHeartbeatData.tags, checkForUpdateRequest.tags);
    }

    @Test
    public void testClientStatusForNonExistingClientIdShouldGiveNotFound() throws Exception {
        given()
                .auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().everything()
                .when()
                .get(ClientResource.CLIENT_PATH + "/{clientId}/status", "non-exising-client-id");
    }

}
