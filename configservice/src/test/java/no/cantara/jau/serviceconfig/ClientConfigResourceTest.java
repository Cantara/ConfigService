package no.cantara.jau.serviceconfig;

import com.jayway.restassured.RestAssured;
import no.cantara.jau.clientconfig.ClientConfigResource;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ClientConfigResourceTest {
    private Main main;
    private String url;
    private final String username = "read";
    private final String password= "baretillesing";
    private String clientId;


    @BeforeClass
    public void startServer() throws InterruptedException {
        new Thread(() -> {
            main = new Main(6644);
            main.start();
        }).start();
        Thread.sleep(2000);
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
        url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH + ClientConfigResource.CLIENTCONFIG_PATH;
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
