package no.cantara.jau.serviceconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.jau.serviceconfig.ServiceConfigResource;
import no.cantara.jau.testsupport.ServiceConfigBuilder;
import no.cantara.jau.testsupport.TestServer;
import no.cantara.jau.serviceconfig.dto.*;
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
    private static final ObjectMapper mapper = new ObjectMapper();
    private Application application;
    private ServiceConfig serviceConfigResponse;
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();

        application = testServer.getAdminClient().registerApplication("ServiceConfigResourceTest");
    }

    @AfterClass
    public void stop() {
        testServer.stop();
    }

    @Test
    public void testCreateServiceConfig() throws Exception {
        ServiceConfig serviceConfig = ServiceConfigBuilder.createServiceConfigDto("ServiceConfigResourceTest", application);
        String jsonRequest = mapper.writeValueAsString(serviceConfig);

        Response response = given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ServiceConfigResource.SERVICECONFIG_PATH, application.id);

        String jsonResponse = response.body().asString();
        serviceConfigResponse = mapper.readValue(jsonResponse, ServiceConfig.class);
        assertNotNull(serviceConfigResponse.getId());
    }

    @Test(dependsOnMethods = "testCreateServiceConfig")
    public void testGetServiceConfig() throws Exception {
        String path = ServiceConfigResource.SERVICECONFIG_PATH + "/{serviceConfigId}";
        Response response = given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path, application.id, serviceConfigResponse.getId());

        String getResponse = response.body().asString();
        ServiceConfig getServiceConfigResponse =  mapper.readValue(getResponse, ServiceConfig.class);
        assertEquals(getServiceConfigResponse.getId(), getServiceConfigResponse.getId());
    }

    @Test(dependsOnMethods = "testGetServiceConfig")
    public void testPutServiceConfig() throws Exception {
        serviceConfigResponse.setName("something new");
        String putJsonRequest = mapper.writeValueAsString(serviceConfigResponse);

        String path = ServiceConfigResource.SERVICECONFIG_PATH + "/" + serviceConfigResponse.getId();
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
        ServiceConfig updatedServiceConfig = mapper.readValue(jsonResponse,ServiceConfig.class);
        assertEquals(updatedServiceConfig.getName(), serviceConfigResponse.getName());
        assertEquals(updatedServiceConfig.getId(), serviceConfigResponse.getId());
    }

    @Test(dependsOnMethods = "testPutServiceConfig")
    public void testDeleteServiceConfig() throws Exception {
        String path = ServiceConfigResource.SERVICECONFIG_PATH + "/{serviceConfigId}";
        given().
                auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .log().everything()
                .expect()
                .statusCode(204)
                .log().ifError()
                .when()
                .delete(path, application.id, serviceConfigResponse.getId());

        given().
                auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().ifError()
                .when()
                .delete(path, application.id, serviceConfigResponse.getId());
    }

    @Test
    public void testFindServiceConfigUnAuthorized() throws Exception {
        //GET
        String path = "/serviceconfig/query";
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
