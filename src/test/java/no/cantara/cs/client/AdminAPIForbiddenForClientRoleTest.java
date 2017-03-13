package no.cantara.cs.client;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import no.cantara.cs.application.ApplicationResource;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.Client;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.util.function.Function;

import static com.jayway.restassured.RestAssured.given;

/**
 * Verify that admin API resources return forbidden for user without admin privileges.
 * https://wiki.cantara.no/display/JAU/ConfigService+Admin+API
 *
 * @author Asbjørn Willersrud
 */
public class AdminAPIForbiddenForClientRoleTest {
    private ConfigServiceClient configServiceClient;
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
        configServiceClient.cleanApplicationState();
    }


    @Test
    public void testGetAdminPathsForbidden() {
        String[] paths = new String[]{
                ApplicationResource.APPLICATION_PATH,
                ApplicationResource.APPLICATION_PATH + "/config",
                ApplicationResource.APPLICATION_PATH + "/app1/status",
                ApplicationResource.APPLICATION_PATH + "/app1/config",
                ApplicationResource.APPLICATION_PATH + "/app1/config/appconfig1",

                ClientResource.CLIENT_PATH,
                ClientResource.CLIENT_PATH + "/1",
                ClientResource.CLIENT_PATH + "/1/env",
                ClientResource.CLIENT_PATH + "/1/status",
                ClientResource.CLIENT_PATH + "/1/config",
                ClientResource.CLIENT_PATH + "/1/events"
        };
        for (String path : paths) {
            expectForbiddenWhen().get(path);
        }
    }

    @Test
    public void testPutClientForbidden() {
        expectForbiddenWhen(request -> request.body(new Client("1", "1", false)).contentType(ContentType.JSON))
                .put(ClientResource.CLIENT_PATH + "/1");
    }

    @Test
    public void testPostApplicationConfigForbidden() {
        expectForbiddenWhen(request -> request.body(new ApplicationConfig("applicationConfigName1")).contentType(ContentType.JSON))
                .post(ApplicationResource.APPLICATION_PATH + "/app1/config");
    }
    @Test
    public void testPutApplicationConfigForbidden() {
        expectForbiddenWhen(request -> request.body(new ApplicationConfig("applicationConfigName1")).contentType(ContentType.JSON))
                .put(ApplicationResource.APPLICATION_PATH + "/app1/config/appconfig1");
    }
    @Test
    public void testDeleteApplicationConfigForbidden() {
        expectForbiddenWhen(request -> request.body(new ApplicationConfig("applicationConfigName1")).contentType(ContentType.JSON))
                .delete(ApplicationResource.APPLICATION_PATH + "/app1/config/appconfig1");
    }


    private ResponseSpecification expectForbiddenWhen() {
        return expectForbiddenWhen(requestSpecification -> requestSpecification);
    }

    private ResponseSpecification expectForbiddenWhen(Function<RequestSpecification, RequestSpecification> requestSpecificationFunction) {
        RequestSpecification requestSpecification = given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD);
        return requestSpecificationFunction.apply(requestSpecification)
                .expect()
                .statusCode(HttpURLConnection.HTTP_FORBIDDEN)
                .log().ifError()
                .when();
    }
}