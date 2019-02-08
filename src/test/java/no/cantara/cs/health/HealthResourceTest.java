package no.cantara.cs.health;

import no.cantara.cs.testsupport.BaseSystemTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.given;

/**
 * @author <a href="mailto:asbjornwillersrud@gmail.com">Asbjørn Willersrud</a> 30/03/2016.
 */
public class HealthResourceTest extends BaseSystemTest {

    @Test
    public void testHealth() throws IOException {
        given()
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().everything()
                .when()
                .get(HealthResource.HEALTH_PATH);
    }

    @Test
    public void testMe() {
    	Properties sysProps = System.getProperties();
    	sysProps.list(System.out);
    	//System.out.println("---------------> " + System.getenv());
    }
}