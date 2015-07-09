package no.cantara.jau;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ServiceConfigResourceTest {
    private Main main;

    @BeforeClass
    public void startServer() {
        main = new Main(6644);
        main.start();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
    }

    //expect there to be a clientConfig with clientId=client1
    @Test
    public void testFindServiceConfigOK() throws Exception {
        //GET
        String path = "/serviceconfig/query";
        Response response = given()
                .queryParam("clientid", "clientid1")
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path);

        String jsonResponse = response.body().asString();
        ServiceConfig serviceConfig = ServiceConfigSerializer.fromJson(jsonResponse);
        assertEquals(serviceConfig.getName(), "Service1-1.23");
        assertNotNull(serviceConfig.getChangedTimestamp());
    }
}
