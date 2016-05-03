package no.cantara.cs.client;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import no.cantara.cs.application.ApplicationResource;
import no.cantara.cs.dto.Client;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.function.Function;

import static com.jayway.restassured.RestAssured.given;

/**
 * @author Asbjørn Willersrud
 */
public class ClientUserAccessTest {

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
    public void testApplicationResourceForbidden() {
        expectForbiddenWhen().get(ApplicationResource.APPLICATION_PATH);
    }

    @Test
    public void testGetAllClientsForbidden() {
        expectForbiddenWhen().get(ClientResource.CLIENT_PATH);
    }

    @Test
    public void testGetClientByIdForbidden() {
        expectForbiddenWhen().get(ClientResource.CLIENT_PATH + "/1");
    }

    @Test
    public void testGetClientEnvForbidden() {
        expectForbiddenWhen().get(ClientResource.CLIENT_PATH + "/1/env");
    }

    @Test
    public void testGetClientStatusForbidden() {
        expectForbiddenWhen().get(ClientResource.CLIENT_PATH + "/1/status");
    }

    @Test
    public void testGetClientConfigForbidden() {
        expectForbiddenWhen().get(ClientResource.CLIENT_PATH + "/1/config");
    }

    @Test
    public void testGetClientEventsForbidden() {
        expectForbiddenWhen().get(ClientResource.CLIENT_PATH + "/1/events");
    }

    @Test
    public void testPutClientForbidden() {
        expectForbiddenWhen(request -> request.body(new Client("1", "1", false)).contentType(ContentType.JSON))
                .put(ClientResource.CLIENT_PATH + "/1");
    }

    private ResponseSpecification expectForbiddenWhen() {
        return expectForbiddenWhen(requestSpecification -> requestSpecification);
    }

    private ResponseSpecification expectForbiddenWhen(Function<RequestSpecification, RequestSpecification> requestSpecificationFunction) {
        RequestSpecification requestSpecification = given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD);
        return requestSpecificationFunction.apply(requestSpecification)
                .expect()
                .statusCode(403)
                .log().ifError()
                .when();
    }

}
