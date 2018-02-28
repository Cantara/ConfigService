package no.cantara.cs.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.BaseSystemTest;
import no.cantara.cs.testsupport.TestServer;
import no.cantara.cs.testsupport.TestServerPostgres;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 * @author Asbjørn Willersrud 2016-03-10
 */
public class ApplicationConfigResourceTest extends BaseSystemTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private Application application;
    private ApplicationConfig config;

    @Test
    public void testCreateApplicationConfig() throws Exception {
        String applicationId = getClass().getSimpleName();
        application = getConfigServiceAdminClient().registerApplication(applicationId);
        ApplicationConfig configInput = ApplicationConfigBuilder.createConfigDto(applicationId, application);
        config = getConfigServiceAdminClient().createApplicationConfig(application, configInput);

        assertNotNull(config.getId());
    }


    @Test(dependsOnMethods = "testCreateApplicationConfig")
    public void testGetApplicationConfigForApplication() throws Exception {
        ApplicationConfig applicationConfig = getConfigServiceAdminClient().getApplicationConfig(application.id);
        assertNotNull(applicationConfig);
        assertEquals(applicationConfig.getId(), config.getId());
    }


    @Test(dependsOnMethods = "testCreateApplicationConfig")
    public void testGetApplicationConfig() throws Exception {
        String path = ApplicationResource.APPLICATION_PATH + ApplicationConfigResource.CONFIG_PATH + "/{configId}";
        Response response = given()
                .auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().ifError()
                .when()
                .get(path, application.id, config.getId());

        String getResponse = response.body().asString();
        ApplicationConfig getConfigResponse =  mapper.readValue(getResponse, ApplicationConfig.class);
        assertEquals(getConfigResponse.getId(), getConfigResponse.getId());
    }

    //TODO: No test for getAllApplicationConfigs


    @Test(dependsOnMethods = "testGetApplicationConfig")
    public void testUpdateApplicationConfig() throws Exception {
        config.setName("something new");
        String putJsonRequest = mapper.writeValueAsString(config);

        String path = ApplicationResource.APPLICATION_PATH + ApplicationConfigResource.CONFIG_PATH + "/" + config.getId();
        Response response = given().
                auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .contentType(ContentType.JSON)
                .body(putJsonRequest)
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_OK)
                .log().ifError()
                .when()
                .put(path, application.id);

        String jsonResponse = response.body().asString();
        ApplicationConfig updatedConfig = mapper.readValue(jsonResponse,ApplicationConfig.class);
        assertEquals(updatedConfig.getName(), config.getName());
        assertEquals(updatedConfig.getId(), config.getId());
    }


    @Test(dependsOnMethods = "testUpdateApplicationConfig")
    public void testDeleteApplicationConfig() throws Exception {
        String path = ApplicationResource.APPLICATION_PATH + ApplicationConfigResource.CONFIG_PATH + "/{configId}";
        given().
                auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_NO_CONTENT)
                .log().ifError()
                .when()
                .delete(path, application.id, config.getId());

        given().
                auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_NOT_FOUND)
                .log().ifError()
                .when()
                .delete(path, application.id, config.getId());
    }


    //See comment in ApplicationConfigResource
    @Test
    public void testRemoveApplication() throws IOException {
        Application app = getConfigServiceAdminClient().registerApplication("AppToBeDeleted");
        assertNotNull(app);

        String path = ApplicationResource.APPLICATION_PATH;
        given()
                .pathParam("appId",app.id)
                .auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_NO_CONTENT)
                .log().ifError()
                .when()
                .delete(path+"/{appId}");
    }
}
