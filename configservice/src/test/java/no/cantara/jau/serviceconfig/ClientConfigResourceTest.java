package no.cantara.jau.serviceconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ClientConfigResourceTest {
    private Main main;
    private final String username = "read";
    private final String password= "baretillesing";
    private static final ObjectMapper mapper = new ObjectMapper();


    @BeforeClass
    public void startServer() throws InterruptedException {
        new Thread(() -> {
            main = new Main(6644);
            main.start();
        }).start();
        Thread.sleep(1500);
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void testRegisterClient() throws Exception {
        ClientRegistration registration = new ClientRegistration("UserAdminService");
        registration.envInfo.putAll(System.getenv());
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

        assertNotNull(clientConfig.clientId);
    }
}
