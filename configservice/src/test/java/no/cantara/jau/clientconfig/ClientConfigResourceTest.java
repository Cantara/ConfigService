package no.cantara.jau.clientconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import jdk.nashorn.internal.ir.RuntimeNode;
import no.cantara.jau.serviceconfig.ApplicationResource;
import no.cantara.jau.serviceconfig.Main;
import no.cantara.jau.serviceconfig.ServiceConfigResource;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import sun.net.www.http.HttpClient;

import javax.ws.rs.NotFoundException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.jayway.restassured.RestAssured.given;
import static org.mockito.Mockito.*;
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
    private ConfigServiceClient configServiceClient;


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
        registration.clientName = "client123";

        ClientConfig clientConfig = configServiceClient.registerClient(registration);
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

    @Test(dependsOnMethods = "testRegisterClient")
    public void testCheckForUpdate() throws Exception {
        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest("checksumHere", System.getenv(), "");
        ClientConfig clientConfig = configServiceClient.checkForUpdate(clientId, checkForUpdateRequest);
        assertNotNull(clientConfig);
        assertEquals(clientConfig.clientId, clientId);
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
        when(clientService.checkForUpdatedClientConfig(clientId, any(CheckForUpdateRequest.class))).thenReturn(null);

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
        when(clientService.checkForUpdatedClientConfig(clientId, any(CheckForUpdateRequest.class))).thenReturn(clientConfig);


        CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest(lastChanged);
        ObjectMapper mapper = new ObjectMapper();
        String jsonRequest = mapper.writeValueAsString(checkForUpdateRequest);



        javax.ws.rs.core.Response response = new ClientConfigResource(clientService).checkForUpdate(clientId, jsonRequest);


        assertEquals(response.getStatus(), javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void testStatusShouldBeAvailableAfterRegisterClient() throws Exception {
        ClientRegistrationRequest registration = new ClientRegistrationRequest("UserAdminService");

        ClientConfig clientConfig = configServiceClient.registerClient(registration);

        String html = getHTML("http://localhost::" + main.getPort() + Main.CONTEXT_PATH + ApplicationResource.APPLICATION_PATH + registration.artifactId + "/status");

        assertTrue(html.contains(clientConfig.clientId));
    }

    public static String getHTML(String urlToRead) throws Exception {
        StringBuilder result = new StringBuilder();
        URL url = new URL(urlToRead);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        return result.toString();
    }


}
