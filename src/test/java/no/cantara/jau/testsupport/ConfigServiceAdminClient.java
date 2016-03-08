package no.cantara.jau.testsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import no.cantara.jau.clientconfig.ClientResource;
import no.cantara.jau.serviceconfig.ApplicationResource;
import no.cantara.jau.serviceconfig.ServiceConfigResource;
import no.cantara.jau.serviceconfig.dto.Application;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;

import java.io.IOException;
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

    public ServiceConfig registerServiceConfig(Application application, ServiceConfig serviceConfig) throws IOException {

        String jsonRequest = mapper.writeValueAsString(serviceConfig);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .post(ServiceConfigResource.SERVICECONFIG_PATH, application.id);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, ServiceConfig.class);
    }

    public ServiceConfig updateServiceConfig(String applicationId, ServiceConfig serviceConfig) throws IOException {

        String jsonRequest = mapper.writeValueAsString(serviceConfig);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .put(ServiceConfigResource.SERVICECONFIG_PATH + "/{serviceConfigId}", applicationId, serviceConfig.getId());

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, ServiceConfig.class);
    }

    public ServiceConfig updateClientConfig(String clientId, ClientConfig newClientConfig) throws IOException {
        String jsonRequest = mapper.writeValueAsString(newClientConfig);
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .body(jsonRequest)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .put(ClientResource.CLIENT_PATH + "/{clientId}/config", clientId);

        String jsonResponse = response.body().asString();
        return mapper.readValue(jsonResponse, ServiceConfig.class);
    }

    public void queryApplicationStatus(String artifactId) {
        given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ApplicationResource.APPLICATION_PATH + "/{artifactId}/status", artifactId);
    }

    public Map<String, ServiceConfig> getAllServiceConfigs() throws IOException {
        Response response = given()
                .auth().basic(username, password)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().everything()
                .when()
                .get(ServiceConfigResource.SERVICECONFIG_PATH, "applicationId-is-not-used-by-the-server");
        return mapper.readValue(response.body().asString(), new TypeReference<Map<String, ServiceConfig>>() {});
    }

}
