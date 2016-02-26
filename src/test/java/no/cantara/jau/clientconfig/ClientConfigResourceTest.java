package no.cantara.jau.clientconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.jau.persistence.MapDbTestSupport;
import no.cantara.jau.serviceconfig.ApplicationResource;
import no.cantara.jau.serviceconfig.Main;
import no.cantara.jau.serviceconfig.ServiceConfigResource;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.*;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ClientConfigResourceTest {
    private static final Logger log = LoggerFactory.getLogger(ClientConfigResourceTest.class);
    private Main main;
    private final String username = "read";
    private final String password= "baretillesing";
    private static final ObjectMapper mapper = new ObjectMapper();
    private ConfigServiceClient configServiceClient;
    private Application testApplication;
    private ClientConfig testClientConfig;

    @BeforeClass
    public void startServer() throws Exception {

        MapDbTestSupport.cleanAllData();

        new Thread(() -> {
            main = new Main(6645);
            main.start();
        }).start();
        Thread.sleep(2000);
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
        String url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH + ClientConfigResource.CLIENTCONFIG_PATH;
        configServiceClient = new ConfigServiceClient(url, username, password);
        addTestData();
    }

    private void addTestData() throws Exception {
        testApplication = createApplication("ClientConfigResourceTestApplication");

        createServiceConfig("first", testApplication);
    }

    private Application createApplication(String artifactId) throws IOException {
        Application application = new Application(artifactId);
        Response createApplicationResponse = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(mapper.writeValueAsString(application))
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ApplicationResource.APPLICATION_PATH);

        return mapper.readValue(createApplicationResponse.body().asString(), Application.class);
    }

    private ServiceConfig createServiceConfig(String identifier, Application application) throws IOException {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", application.artifactId, "2.0.1.Final");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "releases").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);
        EventExtractionConfig extractionConfig = new EventExtractionConfig("jau");
        EventExtractionTag tag = new EventExtractionTag("testtag", "\\bheihei\\b", "logs/blabla.logg");
        extractionConfig.addEventExtractionTag(tag);

        ServiceConfig serviceConfig = new ServiceConfig(metadata.artifactId + "_" + metadata.version + "-"
        + identifier);
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.addEventExtractionConfig(extractionConfig);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());

        String jsonRequest = mapper.writeValueAsString(serviceConfig);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ServiceConfigResource.SERVICECONFIG_PATH, application.id);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, ServiceConfig.class);
    }


    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
        configServiceClient.cleanApplicationState();
    }

    @Test
    public void testRegisterClient() throws Exception {

        ClientRegistrationRequest registration = new ClientRegistrationRequest(testApplication.artifactId);
        registration.envInfo.putAll(System.getenv());
        registration.clientName = "client123";

        this.testClientConfig = configServiceClient.registerClient(registration);
        assertNotNull(testClientConfig);

        ClientConfig clientConfig2 = configServiceClient.registerClient(registration);
        String clientId2 = clientConfig2.clientId;
        assertFalse(testClientConfig.clientId.equalsIgnoreCase(clientId2));
    }

    @Test
    public void testRegisterClientUnknownArtifactId() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UnknownArtifactId");
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
    public void testRegisterClientWithoutServiceConfigShouldReturnNotFound() throws Exception {
        Application applicationWithoutServiceConfig = createApplication("NewArtifactId");

        ClientRegistrationRequest request = new ClientRegistrationRequest(applicationWithoutServiceConfig.artifactId);
        try {
            ClientConfig clientConfig = configServiceClient.registerClient(request);
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

        ClientRegistrationRequest clientRegistrationRequest = new ClientRegistrationRequest("arbitrary-artifact-id");
        ObjectMapper mapper = new ObjectMapper();

        String jsonRequest = mapper.writeValueAsString(clientRegistrationRequest);

        javax.ws.rs.core.Response response = clientConfigResource.registerClient(jsonRequest);

        assertEquals(response.getStatus(), javax.ws.rs.core.Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testCheckForUpdate() throws Exception {
        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest("checksumHere", System.getenv(), "");
        ClientConfig clientConfig = configServiceClient.checkForUpdate(testClientConfig.clientId, checkForUpdateRequest);
        assertNotNull(clientConfig);
        assertEquals(clientConfig.clientId, testClientConfig.clientId);
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testGetExtractionConfigs() {
        configServiceClient.saveApplicationState(testClientConfig);

        List<EventExtractionConfig> tags = configServiceClient.getEventExtractionConfigs();

        log.info(tags.toString());
        Assert.assertEquals(tags.size(), 1);
        Assert.assertEquals(tags.get(0).groupName, "jau");
        Assert.assertEquals(tags.get(0).tags.get(0).filePath, "logs/blabla.logg");
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
        ServiceConfig serviceConfig = createServiceConfig("for-single-client", testApplication);

        ClientRegistrationRequest registration = new ClientRegistrationRequest(testApplication.artifactId);
        ClientConfig clientConfig = configServiceClient.registerClient(registration);

        Assert.assertNotNull(clientConfig.clientId);

        clientConfig.serviceConfig = serviceConfig;

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

    @Test(dependsOnMethods = "testRegisterClient")
    public void testStatusShouldBeAvailableAfterRegisterClient() throws Exception {
        String path = ApplicationResource.APPLICATION_PATH + "/" + testApplication.artifactId + "/status";

        Response response = given()
                .auth().basic(username, password)
                .get(path);

        assertTrue(response.body().asString().contains(testClientConfig.clientId));
    }
}