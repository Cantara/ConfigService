package no.cantara.cs.testsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.cs.application.ApplicationResource;
import no.cantara.cs.application.ApplicationStatus;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.config.ApplicationConfigResource;
import no.cantara.cs.dto.*;

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

    public ApplicationConfig createApplicationConfig(Application application, ApplicationConfig config) throws IOException {

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
                .post(ApplicationConfigResource.CONFIG_PATH, application.id);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, ApplicationConfig.class);
    }

    public ApplicationConfig updateConfig(String applicationId, ApplicationConfig config) throws IOException {

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
                .put(ApplicationConfigResource.CONFIG_PATH + "/{configId}", applicationId, config.getId());

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, ApplicationConfig.class);
    }

    public Client getClient(String clientId) throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ClientResource.CLIENT_PATH + "/{clientId}", clientId);

        return mapper.readValue(response.body().asString(), Client.class);
    }

    public Client putClient(Client client) throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(mapper.writeValueAsString(client))
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .put(ClientResource.CLIENT_PATH + "/{clientId}", client.clientId);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, Client.class);
    }

    public ClientStatus getClientStatus(String clientId) throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ClientResource.CLIENT_PATH + "/{clientId}/status", clientId);

        return mapper.readValue(response.body().asString(), ClientStatus.class);
    }

    public ClientEnvironment getClientEnvironment(String clientId) throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ClientResource.CLIENT_PATH + "/{clientId}/env", clientId);

        return mapper.readValue(response.body().asString(), ClientEnvironment.class);
    }

    public ApplicationStatus getApplicationStatus(String artifactId) throws IOException {
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

    public Map<String, ApplicationConfig> getAllConfigs() throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ApplicationConfigResource.CONFIG_PATH, "applicationId-is-not-used-by-the-server");
        return mapper.readValue(response.body().asString(), new TypeReference<Map<String, ApplicationConfig>>() {});
    }

}
