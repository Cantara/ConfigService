package no.cantara.jau.clientconfig;

import com.jayway.restassured.http.ContentType;
import no.cantara.jau.clientconfig.ClientConfigResource;
import no.cantara.jau.testsupport.ConfigServiceAdminClient;
import no.cantara.jau.testsupport.TestServer;
import no.cantara.jau.testsupport.ServiceConfigBuilder;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.Application;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.NotFoundException;

import java.util.Properties;

import static com.jayway.restassured.RestAssured.config;
import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class RegisterClientTest {

    private ConfigServiceClient configServiceClient;
    private Application application;
    private ClientConfig clientConfig;
    private ConfigServiceAdminClient configServiceAdminClient;

    private TestServer testServer;
    private ServiceConfig serviceConfig;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        application = configServiceAdminClient.registerApplication("RegisterClientTest");
        serviceConfig = configServiceAdminClient.registerServiceConfig(application, ServiceConfigBuilder.createServiceConfigDto("arbitrary-config", application));
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
        registration.clientName = "client123";

        clientConfig = configServiceClient.registerClient(registration);
        assertNotNull(clientConfig);
        assertEquals(clientConfig.serviceConfig.getId(), serviceConfig.getId());
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testSaveApplicationState() {
        configServiceClient.saveApplicationState(clientConfig);
        Properties applicationState = configServiceClient.getApplicationState();

        assertEquals(applicationState.getProperty("clientId"), clientConfig.clientId);
        assertEquals(applicationState.getProperty("lastChanged"), clientConfig.serviceConfig.getLastChanged());
        assertEquals(applicationState.getProperty("command"), clientConfig.serviceConfig.getStartServiceScript());
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testRegisterAnotherClientShouldGetDifferentClientId() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest(application.artifactId);
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client123";

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
    public void testRegisterClientWithoutServiceConfigShouldReturnNotFound() throws Exception {
        Application applicationWithoutServiceConfig = configServiceAdminClient.registerApplication("NewArtifactId");
        ClientRegistrationRequest request = new ClientRegistrationRequest(applicationWithoutServiceConfig.artifactId);
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
                .post(ClientConfigResource.CLIENTCONFIG_PATH);
    }

}
