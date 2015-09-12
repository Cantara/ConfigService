package no.cantara.jau.serviceconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.jau.clientconfig.ClientConfigResource;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ClientConfigResourceTest {
    private Main main;
    private String url;
    private final String username = "read";
    private final String password= "baretillesing";
    private static final ObjectMapper mapper = new ObjectMapper();
    private String clientId;


    @BeforeClass
    public void startServer() throws Exception {
        new Thread(() -> {
            main = new Main(6644);
            main.start();
        }).start();
        Thread.sleep(2000);
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
        url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH + ClientConfigResource.CLIENTCONFIG_PATH;


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

        ServiceConfig serviceConfig = createServiceConfig();

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
    private ServiceConfig createServiceConfig() {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.0.1.Final");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "releases").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);

        ServiceConfig serviceConfig = new ServiceConfig(metadata.artifactId + "_" + metadata.version);
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());
        return serviceConfig;
    }


    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void testRegisterClient() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UserAdminService");
        registration.envInfo.putAll(System.getenv());

        ClientConfig clientConfig = ConfigServiceClient.registerClient(url, username, password, registration);
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

        ClientConfig clientConfig2 = ConfigServiceClient.registerClient(url, username, password, registration2);
        String clientId2 = clientConfig2.clientId;
        assertFalse(clientId.equalsIgnoreCase(clientId2));
    }

    @Test
    public void testRegisterClientUnknownName() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UserService");
        registration.envInfo.putAll(System.getenv());

        ClientConfig clientConfig = ConfigServiceClient.registerClient(url, username, password, registration);
        assertNull(clientConfig);
    }

    @Test(dependsOnMethods = "testRegisterClient")
    public void testCheckForUpdate() throws Exception {
        ClientConfig clientConfig = ConfigServiceClient.checkForUpdate(url, username, password, clientId, "checksumHere", System.getenv());
        assertNotNull(clientConfig);
        assertEquals(clientConfig.clientId, clientId);
    }
}
