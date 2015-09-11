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
import static org.testng.Assert.assertTrue;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-02-01
 */
public class ServiceConfigResourceTest {
    private Main main;
    private final String username = "read";
    private final String password= "baretillesing";
    private static final ObjectMapper mapper = new ObjectMapper();


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
    public void testCreateServiceConfig() throws Exception {
        ServiceConfig serviceConfigResponse = createServiceConfigResponse();
        assertNotNull(serviceConfigResponse.getId());
    }


    private ServiceConfig createServiceConfigResponse() throws Exception{
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", "UserAdminService", "2.0.1.Final");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "releases").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);

        ServiceConfig serviceConfig = new ServiceConfig(metadata.artifactId + "_" + metadata.version);
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());

        String path = "/serviceconfig";
        String jsonRequest = mapper.writeValueAsString(serviceConfig);
        //ServiceConfigSerializer.toJson(serviceConfig);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(path);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, ServiceConfig.class);
        //ServiceConfigSerializer.fromJson(jsonResponse);
    }

    @Test
    public void testGetServiceConfig() throws Exception {
        ServiceConfig serviceConfigResponse = createServiceConfigResponse();

        String path = "/serviceconfig/" + serviceConfigResponse.getId();
        Response response = given()
                .auth().basic(username, password)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path);

        String getResponse = response.body().asString();
        ServiceConfig getServiceConfigResponse =  mapper.readValue(getResponse, ServiceConfig.class);
        //ServiceConfigSerializer.fromJson(getResponse);
        assertEquals(getServiceConfigResponse.getId(), getServiceConfigResponse.getId());
    }

    @Test
    public void testDeleteServiceConfig() throws Exception {
        ServiceConfig serviceConfigResponse = createServiceConfigResponse();

        String path = "/serviceconfig/" + serviceConfigResponse.getId();
        Response response = given().
                auth().basic(username, password)
                .log().everything()
                .expect()
                .statusCode(204)
                .log().ifError()
                .when()
                .delete(path);

        path = "/serviceconfig/" + serviceConfigResponse.getId();
        response = given().
                auth().basic(username, password)
                .log().everything()
                .expect()
                .statusCode(404)
                .log().ifError()
                .when()
                .delete(path);
    }

    @Test
    public void testPutServiceConfig() throws Exception {
        ServiceConfig serviceConfigResponse = createServiceConfigResponse();

        serviceConfigResponse.setName("something new");
        String putJsonRequest = mapper.writeValueAsString(serviceConfigResponse);
        //ServiceConfigSerializer.toJson(serviceConfigResponse);

        String path = "/serviceconfig";
        Response response = given().
                auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(putJsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .put(path);

        String jsonResponse = response.body().asString();
        ServiceConfig updatedServiceConfig = mapper.readValue(jsonResponse,ServiceConfig.class);
        //ServiceConfigSerializer.fromJson(jsonResponse);
        assertEquals(updatedServiceConfig.getName(), serviceConfigResponse.getName());
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

    /*
    @Test
    public void testFindServiceConfigOK() throws Exception {
        //GET
        String path = "/serviceconfig/query";
        Response response = given()
                .auth().basic(username, password)
                .queryParam("clientid", "clientid1")
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .get(path);

        String jsonResponse = response.body().asString();
        ServiceConfig serviceConfig = ServiceConfigSerializer.fromJson(jsonResponse);
        assertEquals(serviceConfig.getName(), "Service1-1.23");
        assertNotNull(serviceConfig.getChangedTimestamp());
    }
    */
}
