package no.cantara.cs.client;

import io.restassured.http.ContentType;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.testsupport.BaseSystemTest;
import no.cantara.cs.testsupport.TestServer;
import org.apache.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class CheckForUpdateTest extends BaseSystemTest {

    private ConfigServiceClient configServiceClient;

    @BeforeClass
    public void startServer() throws Exception {
        configServiceClient = getConfigServiceClient();
    }

    @Test
    public void testCheckForUpdateBrokenJson() {
        given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .contentType(ContentType.JSON)
                .body("{broken json}")
                .log().everything()
                .expect()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().ifError()
                .when()
                .post(ClientResource.CLIENT_PATH + "/{clientId}/sync", 1);
    }

    @Test
    public void testCheckForUpdateUnregisteredClient() throws IOException {
        try {
            configServiceClient.checkForUpdate("non-existing-client-id", new CheckForUpdateRequest(""));
            fail();
        } catch (HttpException e) {
            assertEquals(e.getStatusCode(), HttpStatus.SC_PRECONDITION_FAILED);
        }
    }

}
