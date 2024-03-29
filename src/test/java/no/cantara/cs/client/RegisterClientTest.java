package no.cantara.cs.client;

import io.restassured.http.ContentType;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.BaseSystemTest;
import no.cantara.cs.testsupport.TestServer;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class RegisterClientTest extends BaseSystemTest {

    private ConfigServiceClient configServiceClient;
    private Application application;
    private ClientConfig clientConfig;
    private ConfigServiceAdminClient configServiceAdminClient;

    private ApplicationConfig defaultConfig;

    @BeforeClass
    public void startServer() throws Exception {
        configServiceClient = getConfigServiceClient();

        configServiceAdminClient = getConfigServiceAdminClient();
        application = configServiceAdminClient.registerApplication("RegisterClientTest");
        defaultConfig = configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("default-config", application));
    }

    @Test
    public void testRegisterClient() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest(application.artifactId);
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client-1-name";

        clientConfig = configServiceClient.registerClient(registration);
        assertNotNull(clientConfig);
        assertEquals(clientConfig.config.getId(), defaultConfig.getId());
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testSaveApplicationState() {
        configServiceClient.saveApplicationState(clientConfig);
        Properties applicationState = configServiceClient.getApplicationState();

        assertEquals(applicationState.getProperty("clientId"), clientConfig.clientId);
        assertEquals(applicationState.getProperty("lastChanged"), clientConfig.config.getLastChanged());
        assertEquals(applicationState.getProperty("command"), clientConfig.config.getStartServiceScript());
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testRegisterAnotherClientShouldGetDifferentClientId() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest(application.artifactId);
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client-2-name";

        ClientConfig secondClientConfig = configServiceClient.registerClient(registration);
        String clientId2 = secondClientConfig.clientId;

        assertFalse(clientConfig.clientId.equalsIgnoreCase(clientId2));
    }

    @Test
    public void testRegisterClientUnknownArtifactId() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UnknownArtifactId");
        registration.envInfo.putAll(System.getenv());
        try {
            configServiceClient.registerClient(registration);
            fail("Expected registerClient to fail with unknown artifact");
        } catch (HttpException e) {
            assertEquals(e.getStatusCode(), HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    @Test
    public void testRegisterClientWithoutConfigShouldReturnNotFound() throws Exception {
        Application applicationWithoutConfig = configServiceAdminClient.registerApplication("NewArtifactId");
        ClientRegistrationRequest request = new ClientRegistrationRequest(applicationWithoutConfig.artifactId);
        try {
            configServiceClient.registerClient(request);
            fail("Expected registerClient to fail without config");
        } catch (HttpException e) {
            assertEquals(e.getStatusCode(), HttpURLConnection.HTTP_NOT_FOUND);
        }
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testRegisterClientWithExistingClientId() throws Exception {
        ClientRegistrationRequest request = new ClientRegistrationRequest(application.artifactId);
        request.clientId = clientConfig.clientId;

        ClientConfig newClientConfig = configServiceClient.registerClient(request);

        assertNotNull(newClientConfig);
        assertEquals(newClientConfig.config.getId(), clientConfig.config.getId());
        assertEquals(newClientConfig.clientId, request.clientId);
    }

    @Test
    public void testBrokenJsonShouldReturnBadRequest() throws Exception {
        given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .contentType(ContentType.JSON)
                .body("{broken json}")
                .log().everything()
                .expect()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().ifError()
                .when()
                .post(ClientResource.CLIENT_PATH + "/registration");
    }

}
