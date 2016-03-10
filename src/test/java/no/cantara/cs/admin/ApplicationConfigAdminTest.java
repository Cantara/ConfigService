package no.cantara.cs.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.cs.config.ConfigResource;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.Config;
import no.cantara.cs.testsupport.ConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ApplicationConfigAdminTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private Application application;
    private Config config;
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test
    public void testRegisterApplication() throws IOException {
        application = testServer.getAdminClient().registerApplication("ApplicationConfigAdminTest");
    }

    @Test(dependsOnMethods = "testRegisterApplication")
    public void testCreateConfig() throws Exception {
        Config configInput = ConfigBuilder.createConfigDto("ApplicationConfigAdminTest", application);

        config = testServer.getAdminClient().registerConfig(application, configInput);
        assertNotNull(config.getId());
    }

    @Test(dependsOnMethods = "testCreateConfig")
    public void testGetConfig() throws Exception {
        String path = ConfigResource.CONFIG_PATH + "/{serviceConfigId}";
        Response response = given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path, application.id, config.getId());

        String getResponse = response.body().asString();
        Config getConfigResponse =  mapper.readValue(getResponse, Config.class);
        assertEquals(getConfigResponse.getId(), getConfigResponse.getId());
    }

    @Test(dependsOnMethods = "testGetConfig")
    public void testPutConfig() throws Exception {
        config.setName("something new");
        String putJsonRequest = mapper.writeValueAsString(config);

        String path = ConfigResource.CONFIG_PATH + "/" + config.getId();
        Response response = given().
                auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .contentType(ContentType.JSON)
                .body(putJsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .put(path, application.id);

        String jsonResponse = response.body().asString();
        Config updatedConfig = mapper.readValue(jsonResponse,Config.class);
        assertEquals(updatedConfig.getName(), config.getName());
        assertEquals(updatedConfig.getId(), config.getId());
    }

    @Test(dependsOnMethods = "testPutConfig")
    public void testDeleteConfig() throws Exception {
        String path = ConfigResource.CONFIG_PATH + "/{serviceConfigId}";
        given().
                auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .log().everything()
                .expect()
                .statusCode(204)
                .log().ifError()
                .when()
                .delete(path, application.id, config.getId());

        given().
                auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().ifError()
                .when()
                .delete(path, application.id, config.getId());
    }

    @Test
    public void testFindConfigUnAuthorized() throws Exception {
        //GET
        String path = "/config/query";
        given()
                .queryParam("clientid", "clientid1")
                .log().everything()
                .expect()
                .statusCode(401)
                .log().ifError()
                .when()
                .get(path);
    }


}
