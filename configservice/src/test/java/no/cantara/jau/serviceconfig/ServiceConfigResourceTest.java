package no.cantara.jau.serviceconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
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
    private Main main;
    private final String username = "read";
    private final String password= "baretillesing";
    private static final ObjectMapper mapper = new ObjectMapper();
    private Application applicationResponse;
    private ServiceConfig serviceConfigResponse;


    @BeforeClass
    public void startServer() throws InterruptedException {
        new Thread(() -> {
            main = new Main(6644);
            main.start();
        }).start();
        Thread.sleep(1000);
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
    }


    @Test
    public void testCreateApplication() throws Exception {
        Application application = new Application("UserAdminService");
        String jsonRequest = mapper.writeValueAsString(application);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ApplicationResource.APPLICATION_PATH);

        String jsonResponse = response.body().asString();
        applicationResponse = mapper.readValue(jsonResponse, Application.class);
        assertNotNull(applicationResponse.id);
        assertEquals(applicationResponse.artifactId, application.artifactId);
    }



    @Test
    public void testCreateServiceConfig() throws Exception {
        ServiceConfig serviceConfig = createServiceConfig();
        String jsonRequest = mapper.writeValueAsString(serviceConfig);

        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(ServiceConfigResource.SERVICECONFIG_PATH, applicationResponse.id);

        String jsonResponse = response.body().asString();
        serviceConfigResponse = mapper.readValue(jsonResponse, ServiceConfig.class);
        assertNotNull(serviceConfigResponse.getId());
    }

    private ServiceConfig createServiceConfig() {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.0.1.Final");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "releases").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);

        ServiceConfig serviceConfig = new ServiceConfig(metadata.artifactId + "_" + metadata.version);
        serviceConfig.addDownloadItem(downloadItem);
        EventExtractionConfig extractionConfig = new EventExtractionConfig("jau");
        EventExtractionTag tag = new EventExtractionTag("testtag", "\\bheihei\\b", "logs/blabla.logg");
        extractionConfig.addEventExtractionTag(tag);
        serviceConfig.addEventExtractionConfig(extractionConfig);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());
        return serviceConfig;
    }


    @Test(dependsOnMethods = "testCreateServiceConfig")
    public void testGetServiceConfig() throws Exception {
        String path = ServiceConfigResource.SERVICECONFIG_PATH + "/{serviceConfigId}";
        Response response = given()
                .auth().basic(username, password)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path, applicationResponse.id, serviceConfigResponse.getId());

        String getResponse = response.body().asString();
        ServiceConfig getServiceConfigResponse =  mapper.readValue(getResponse, ServiceConfig.class);
        //ServiceConfigSerializer.fromJson(getResponse);
        assertEquals(getServiceConfigResponse.getId(), getServiceConfigResponse.getId());
    }

    @Test(dependsOnMethods = "testGetServiceConfig")
    public void testPutServiceConfig() throws Exception {
        serviceConfigResponse.setName("something new");
        String putJsonRequest = mapper.writeValueAsString(serviceConfigResponse);

        String path = ServiceConfigResource.SERVICECONFIG_PATH + "/" + serviceConfigResponse.getId();
        Response response = given().
                auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(putJsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .put(path, applicationResponse.id);

        String jsonResponse = response.body().asString();
        ServiceConfig updatedServiceConfig = mapper.readValue(jsonResponse,ServiceConfig.class);
        assertEquals(updatedServiceConfig.getName(), serviceConfigResponse.getName());
        assertEquals(updatedServiceConfig.getId(), serviceConfigResponse.getId());
    }

    @Test(dependsOnMethods = "testPutServiceConfig")
    public void testDeleteServiceConfig() throws Exception {
        String path = ServiceConfigResource.SERVICECONFIG_PATH + "/{serviceConfigId}";
        Response response = given().
                auth().basic(username, password)
                .log().everything()
                .expect()
                .statusCode(204)
                .log().ifError()
                .when()
                .delete(path, applicationResponse.id, serviceConfigResponse.getId());

        //path = "/serviceconfig/" + serviceConfigResponse.getId();
        response = given().
                auth().basic(username, password)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().ifError()
                .when()
                .delete(path, applicationResponse.id, serviceConfigResponse.getId());
    }



    //expect there to be a clientConfig with clientId=client1
    @Test
    public void testFindServiceConfigUnAuthorized() throws Exception {
        //GET
        String path = "/serviceconfig/query";
        Response response = given()
                .queryParam("clientid", "clientid1")
                .log().everything()
                .expect()
                .statusCode(401)
                .log().ifError()
                .when()
                .get(path);
    }


}
