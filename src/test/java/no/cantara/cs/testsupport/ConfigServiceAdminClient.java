package no.cantara.cs.testsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.config.ApplicationResource;
import no.cantara.cs.config.ConfigResource;
import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.Config;
import no.cantara.cs.testsupport.dto.ApplicationStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;

public class ConfigServiceAdminClient {

    private static final ObjectMapper mapper = new ObjectMapper();
    private String username;
    private String password;

    public ConfigServiceAdminClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Application registerApplication(String artifactId) throws IOException {
        Application application = new Application(artifactId);
        Response createApplicationResponse = given()

                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(mapper.writeValueAsString(application))
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .post(ApplicationResource.APPLICATION_PATH);

        return mapper.readValue(createApplicationResponse.body().asString(), Application.class);
    }

    public Config registerConfig(Application application, Config config) throws IOException {

        String jsonRequest = mapper.writeValueAsString(config);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .post(ConfigResource.CONFIG_PATH, application.id);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, Config.class);
    }

    public Config updateConfig(String applicationId, Config config) throws IOException {

        String jsonRequest = mapper.writeValueAsString(config);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .put(ConfigResource.CONFIG_PATH + "/{configId}", applicationId, config.getId());

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, Config.class);
    }

    public Config updateClientConfig(String clientId, String configId) throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .put(ClientResource.CLIENT_PATH + "/{clientId}/config/{configId}", clientId, configId);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, Config.class);
    }

    public ApplicationStatus queryApplicationStatus(String artifactId) throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ApplicationResource.APPLICATION_PATH + "/{artifactId}/status", artifactId);

        return mapper.readValue(response.body().asString(), ApplicationStatus.class);
    }

    public List<Application> getAllApplications() throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ApplicationResource.APPLICATION_PATH);
        return mapper.readValue(response.body().asString(), new TypeReference<List<Application>>() {});
    }

    public Map<String, Config> getAllConfigs() throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ConfigResource.CONFIG_PATH, "applicationId-is-not-used-by-the-server");
        return mapper.readValue(response.body().asString(), new TypeReference<Map<String, Config>>() {});
    }

}
