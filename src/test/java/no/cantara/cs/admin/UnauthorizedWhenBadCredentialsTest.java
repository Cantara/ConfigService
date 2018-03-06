package no.cantara.cs.admin;

import com.jayway.restassured.specification.RequestSpecification;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.testsupport.BaseSystemTest;
import no.cantara.cs.testsupport.TestConstants;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.jayway.restassured.RestAssured.given;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2017-04-20
 */
public class UnauthorizedWhenBadCredentialsTest extends BaseSystemTest {
    //Open endpoints
    //There is currently only one, which is tested by HealthResourceTest.testHealth.

    @Test
    public void testUnauthorizedWhenNoCredentials() {
        expectUnauthorizedForPaths(() -> given()
                .auth().none());
    }

    @Test
    public void testUnauthorizedWhenNonExistingUser()  {
        expectUnauthorizedForPaths(() -> given()
                .auth().basic("nonExistingUsername", TestServer.PASSWORD));
    }

    @Test
    public void testUnauthorizedWhenWrongPassword() {
        expectUnauthorizedForPaths(() -> given()
                .auth().basic(TestServer.USERNAME, "wrongPassword"));
    }

    private void expectUnauthorizedForPaths(Supplier<RequestSpecification> auth) {
        List<String> restrictedPaths = new ArrayList<>();
        restrictedPaths.addAll(Arrays.asList(TestConstants.ADMIN_PATHS));
        restrictedPaths.add(ClientResource.CLIENT_PATH + "/registration");
        restrictedPaths.add(ClientResource.CLIENT_PATH + "/1/sync");

        for (String path : restrictedPaths) {
            auth.get()
                    .expect()
                    .statusCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                    .log().ifError()
                    .when()
                    .get(path);
        }
    }
}
