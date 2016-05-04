package no.cantara.cs.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.cs.application.ApplicationResource;
import no.cantara.cs.config.ApplicationConfigResource;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ApplicationConfigAdminTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private Application application;
    private ApplicationConfig config;
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
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
    public void testRegisterApplicationWithDuplicateArtifactIdShouldReturnBadRequest() throws IOException {
        Application application1 = new Application("ApplicationConfigAdminTest");
        given()
                .auth().basic(TestServer.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .contentType(ContentType.JSON)
                .body(mapper.writeValueAsString(application1))
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .log().everything()
                .when()
                .post(ApplicationResource.APPLICATION_PATH);
    }

    @Test(dependsOnMethods = "testRegisterApplication")
    public void testCreateConfig() throws Exception {
        ApplicationConfig configInput = ApplicationConfigBuilder.createConfigDto("ApplicationConfigAdminTest", application);

        config = testServer.getAdminClient().createApplicationConfig(application, configInput);
        assertNotNull(config.getId());
    }

    @Test(dependsOnMethods = "testCreateConfig")
    public void testGetApplicationConfig() throws Exception {
        ApplicationConfig applicationConfig = testServer.getAdminClient().getApplicationConfig(application.id);
        assertNotNull(applicationConfig);
        assertEquals(applicationConfig.getId(), config.getId());
    }

    @Test(dependsOnMethods = "testRegisterApplication")
    public void testGetApplications() throws IOException {
        List<Application> applications = testServer.getAdminClient().getAllApplications();
        assertNotNull(applications);
        assertEquals(applications.size(), 1);
        Application application = applications.iterator().next();
        assertEquals(application.artifactId, this.application.artifactId);
        assertEquals(application.id, this.application.id);
    }

    @Test(dependsOnMethods = "testCreateConfig")
    public void testGetConfig() throws Exception {
        String path = ApplicationResource.APPLICATION_PATH + ApplicationConfigResource.CONFIG_PATH + "/{configId}";
        Response response = given()
                .auth().basic(TestServer.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path, application.id, config.getId());

        String getResponse = response.body().asString();
        ApplicationConfig getConfigResponse =  mapper.readValue(getResponse, ApplicationConfig.class);
        assertEquals(getConfigResponse.getId(), getConfigResponse.getId());
    }

    @Test(dependsOnMethods = "testGetConfig")
    public void testPutConfig() throws Exception {
        config.setName("something new");
        String putJsonRequest = mapper.writeValueAsString(config);

        String path = ApplicationResource.APPLICATION_PATH + ApplicationConfigResource.CONFIG_PATH + "/" + config.getId();
        Response response = given().
                auth().basic(TestServer.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .contentType(ContentType.JSON)
                .body(putJsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .put(path, application.id);

        String jsonResponse = response.body().asString();
        ApplicationConfig updatedConfig = mapper.readValue(jsonResponse,ApplicationConfig.class);
        assertEquals(updatedConfig.getName(), config.getName());
        assertEquals(updatedConfig.getId(), config.getId());
    }

    @Test(dependsOnMethods = "testPutConfig")
    public void testDeleteConfig() throws Exception {
        String path = ApplicationResource.APPLICATION_PATH + ApplicationConfigResource.CONFIG_PATH + "/{configId}";
        given().
                auth().basic(TestServer.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .log().everything()
                .expect()
                .statusCode(204)
                .log().ifError()
                .when()
                .delete(path, application.id, config.getId());

        given().
                auth().basic(TestServer.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
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
