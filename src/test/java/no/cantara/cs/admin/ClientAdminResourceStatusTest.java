package no.cantara.cs.admin;

import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.dto.*;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.BaseSystemTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.testng.Assert.*;

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
        
       
        assertEquals(latestClientHeartbeatData.clientName, registrationRequest.clientName);
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
        
        
        assertEquals(latestClientHeartbeatData.clientName, checkForUpdateRequest.clientName);
        assertEquals(latestClientHeartbeatData.tags, checkForUpdateRequest.tags);
    }

    @Test
    public void testClientStatusForNonExistingClientIdShouldGiveNotFound() throws Exception {
        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(application.artifactId);
        registrationRequest.envInfo = new HashMap<>();
        registrationRequest.envInfo.put("var1", "value1");
        clientConfig = getTestServer().getConfigServiceClient().registerClient(registrationRequest);

        ClientStatus clientStatus = getTestServer().getAdminClient().getClientStatus("non-existing-client-id");
        assertNotNull(clientStatus);
        assertNull(clientStatus.client);
        assertNull(clientStatus.latestClientHeartbeatData);
//        given()
//                .auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
//                .contentType(MediaType.APPLICATION_JSON)
//                .log().everything()
//                .expect()
//                .statusCode(404)
//                .log().everything()
//                .when()
//                .get(ClientResource.CLIENT_PATH + "/{clientId}/status", "non-exising-client-id");
    }

}
