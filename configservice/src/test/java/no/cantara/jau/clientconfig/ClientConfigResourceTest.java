package no.cantara.jau.clientconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.jau.serviceconfig.ApplicationResource;
import no.cantara.jau.serviceconfig.Main;
import no.cantara.jau.serviceconfig.ServiceConfigResource;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 * TODO: Tests were made for InMemPersistance. Using MapDB for persistence, several tests often fail
 * due to race conditions when using the same db
 */
public class ClientConfigResourceTest {
    private static final Logger log = LoggerFactory.getLogger(ClientConfigResourceTest.class);
    private Main main;
    private String url;
    private final String username = "read";
    private final String password= "baretillesing";
    private static final ObjectMapper mapper = new ObjectMapper();
    private String clientId;
    private ConfigServiceClient configServiceClient;
    private String applicationId;


    @BeforeClass
    public void startServer() throws Exception {
        new Thread(() -> {
            main = new Main(6645);
            main.start();
        }).start();
        Thread.sleep(2000);
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
        url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH + ClientConfigResource.CLIENTCONFIG_PATH;

        configServiceClient = new ConfigServiceClient(url, username, password);
        addTestData();
    }

    private void addTestData() throws Exception {
        Application application = new Application("UserAdminService");
        String jsonRequest = mapper.writeValueAsString(application);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ApplicationResource.APPLICATION_PATH);

        String jsonResponse = response.body().asString();
        Application applicationResponse = mapper.readValue(jsonResponse, Application.class);
        applicationId = applicationResponse.id;

        ServiceConfig serviceConfig = createServiceConfig("first");

        String jsonRequest2 = mapper.writeValueAsString(serviceConfig);
        Response response2 = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest2)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ServiceConfigResource.SERVICECONFIG_PATH, applicationResponse.id);
    }
    private ServiceConfig createServiceConfig(String identifier) {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.0.1.Final");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "releases").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);
        EventExtractionTag extractionTag = new EventExtractionTag("testtag");
        extractionTag.addEventExtractionItem(new EventExtractionItem("\\bfoobar\\b", "path/to/log/file.log"));

        ServiceConfig serviceConfig = new ServiceConfig(metadata.artifactId + "_" + metadata.version + "-"
        + identifier);
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.addEventExtractionTag(extractionTag);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());
        return serviceConfig;
    }


    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
        configServiceClient.cleanApplicationState();
    }

    // Test fails in Jenkins due to mapdb persistence not handled correctly in tests
    @Test(enabled=false)
    public void testRegisterClient() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UserAdminService");
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client123";

        ClientConfig clientConfig = configServiceClient.registerClient(registration);
        configServiceClient.saveApplicationState(clientConfig);
        /*
        String jsonRequest = mapper.writeValueAsString(registration);
        String path = "/clientconfig";
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(path);
        String jsonResponse = response.body().asString();
        ClientConfig clientConfig = mapper.readValue(jsonResponse, ClientConfig.class);
        */

        clientId = clientConfig.clientId;
        assertNotNull(clientId);

        ClientRegistrationRequest registration2 = new ClientRegistrationRequest("UserAdminService");
        registration2.envInfo.putAll(System.getenv());
        ClientConfig clientConfig2 = configServiceClient.registerClient(registration2);

        String clientId2 = clientConfig2.clientId;
        assertFalse(clientId.equalsIgnoreCase(clientId2));
    }

    @Test
    public void testRegisterClientUnknownName() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UserService");
        registration.envInfo.putAll(System.getenv());

        try {
            ClientConfig clientConfig = configServiceClient.registerClient(registration);
            fail("Should not get this far.");
        } catch (NotFoundException e) {
            assertNotNull(e);
        } catch (Exception e) {
            fail("Should not get another exception.");
        }
    }

    @Test
    public void testBrokenJsonShouldReturnBadRequest() throws Exception {
        javax.ws.rs.core.Response response = new ClientConfigResource(null).registerClient("{broken json");

        assertEquals(response.getStatus(), javax.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testLackingInformationShouldReturnBadRequest() throws JsonProcessingException {
        ClientService clientService = mock(ClientService.class);
        when(clientService.registerClient(any())).thenThrow(IllegalArgumentException.class);
        ClientConfigResource clientConfigResource = new ClientConfigResource(clientService);

        ClientRegistrationRequest clientRegistrationRequest = new ClientRegistrationRequest("UserAdminService");
        ObjectMapper mapper = new ObjectMapper();

        String jsonRequest = mapper.writeValueAsString(clientRegistrationRequest);

        javax.ws.rs.core.Response response = clientConfigResource.registerClient(jsonRequest);

        assertEquals(response.getStatus(), javax.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode());
    }

    // Test fails in Jenkins due to mapdb persistence not handled correctly in tests
    @Test(dependsOnMethods = "testRegisterClient", enabled=false)
    public void testCheckForUpdate() throws Exception {
        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest("checksumHere", System.getenv(), "");
        ClientConfig clientConfig = configServiceClient.checkForUpdate(clientId, checkForUpdateRequest);
        assertNotNull(clientConfig);
        assertEquals(clientConfig.clientId, clientId);
    }

    @Test(dependsOnMethods = "testRegisterClient", enabled=false)
    public void testGetExtractionConfigs() {
        List<EventExtractionTag> tags = configServiceClient.getEventExtractionTags();
        Assert.assertEquals(tags.size(), 1);
    }

    @Test
    public void testCheckForUpdateBrokenJson() {
        javax.ws.rs.core.Response response = new ClientConfigResource(null).checkForUpdate("", "{broken json");

        assertEquals(response.getStatus(), javax.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testCheckForUpdateUnregisteredClient() {
        ClientService clientService = mock(ClientService.class);
        String clientId = "";
        when(clientService.checkForUpdatedClientConfig(anyString(), any(CheckForUpdateRequest.class))).thenReturn(null);

        javax.ws.rs.core.Response response = new ClientConfigResource(clientService).checkForUpdate(clientId, "{}");

        assertEquals(response.getStatus(), javax.ws.rs.core.Response.Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testCheckForUpdateUnchangedClientConfig() throws IOException {
        String clientId = "Bar";
        String lastChanged = "Foo";

        ServiceConfig serviceConfig = mock(ServiceConfig.class);
        when(serviceConfig.getLastChanged()).thenReturn(lastChanged);

        ClientConfig clientConfig = new ClientConfig(clientId, serviceConfig);

        ClientService clientService = mock(ClientService.class);
        when(clientService.checkForUpdatedClientConfig(anyString(), any(CheckForUpdateRequest.class))).thenReturn(clientConfig);


        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest(lastChanged);
        ObjectMapper mapper = new ObjectMapper();
        String jsonRequest = mapper.writeValueAsString(checkForUpdateRequest);



        javax.ws.rs.core.Response response = new ClientConfigResource(clientService).checkForUpdate(clientId, jsonRequest);


        assertEquals(response.getStatus(), javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void testChangeServiceConfigForSingleClient() throws IOException {
        ServiceConfig serviceConfig = createServiceConfig("for-single-client");
        String jsonRequestServiceConfig = mapper.writeValueAsString(serviceConfig);
        Response newServiceConfigResponse = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequestServiceConfig)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ServiceConfigResource.SERVICECONFIG_PATH, applicationId);

        String getResponse = newServiceConfigResponse.body().asString();
        ServiceConfig getServiceConfigResponse =  mapper.readValue(getResponse, ServiceConfig.class);

        ClientRegistrationRequest registration = new ClientRegistrationRequest("UserAdminService");
        ClientConfig clientConfig = configServiceClient.registerClient(registration);

        Assert.assertNotNull(clientConfig.clientId);

        clientConfig.serviceConfig = getServiceConfigResponse;

        String jsonNewClientConfig = mapper.writeValueAsString(clientConfig);

        Response changeServiceConfigForSingleClientResponse = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonNewClientConfig)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .put(ClientResource.CLIENT_PATH + "/{clientId}/config", clientConfig.clientId);
    }

    // Test fails in Jenkins due to mapdb persistence not handled correctly in tests
    @Test(enabled=false)
    public void testStatusShouldBeAvailableAfterRegisterClient() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UserAdminService");

        ClientConfig clientConfig = configServiceClient.registerClient(registration);

        String path = ApplicationResource.APPLICATION_PATH + "/UserAdminService/status";

        Response response = given()
                .auth().basic(username, password)
                .get(path);

        assertTrue(response.body().asString().contains(clientConfig.clientId));
    }
}