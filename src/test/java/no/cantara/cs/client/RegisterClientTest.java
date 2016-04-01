package no.cantara.cs.client;

import com.jayway.restassured.http.ContentType;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class RegisterClientTest {

    private ConfigServiceClient configServiceClient;
    private Application application;
    private ClientConfig clientConfig;
    private ConfigServiceAdminClient configServiceAdminClient;

    private TestServer testServer;
    private ApplicationConfig defaultConfig;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        configServiceAdminClient = testServer.getAdminClient();
        application = configServiceAdminClient.registerApplication("RegisterClientTest");
        defaultConfig = configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("default-config", application));
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
        configServiceClient.cleanApplicationState();
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

    @Test(expectedExceptions = NotFoundException.class)
    public void testRegisterClientUnknownArtifactId() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UnknownArtifactId");
        registration.envInfo.putAll(System.getenv());
        configServiceClient.registerClient(registration);
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testRegisterClientWithoutConfigShouldReturnNotFound() throws Exception {
        Application applicationWithoutConfig = configServiceAdminClient.registerApplication("NewArtifactId");
        ClientRegistrationRequest request = new ClientRegistrationRequest(applicationWithoutConfig.artifactId);
        configServiceClient.registerClient(request);
    }

    @Test(dependsOnMethods = "testRegisterClient", expectedExceptions = BadRequestException.class)
    public void testRegisterClientWithExistingClientIdShouldFail() throws Exception {
        ClientRegistrationRequest request = new ClientRegistrationRequest(application.artifactId);
        request.clientId = clientConfig.clientId;
        configServiceClient.registerClient(request);
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
