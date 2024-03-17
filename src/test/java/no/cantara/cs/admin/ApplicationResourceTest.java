package no.cantara.cs.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import no.cantara.cs.dto.*;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.BaseSystemTest;
import no.cantara.cs.testsupport.TestServer;
import no.cantara.cs.testsupport.TestServerPostgres;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * @author Asbjørn Willersrud 2016-03-10
 */
public class ApplicationResourceTest extends BaseSystemTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private Application application;

    //createApplication
    @Test
    public void testRegisterApplicationOK() throws IOException {
        application = getConfigServiceAdminClient().registerApplication(getClass().getSimpleName());
    }

    @Test(dependsOnMethods = "testRegisterApplicationOK")
    public void testRegisterApplicationWithDuplicateArtifactIdShouldReturnBadRequest() throws IOException {
        Application application1 = new Application(getClass().getSimpleName());
        given()
                .auth().basic(TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD)
                .contentType(ContentType.JSON)
                .body(mapper.writeValueAsString(application1))
                .log().everything()
                .expect()
                .statusCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .log().everything()
                .when()
                .post(ApplicationResource.APPLICATION_PATH);
    }


    //getAllApplications
    @Test(dependsOnMethods = "testRegisterApplicationOK")
    public void testGetApplications() throws IOException {
        List<Application> applications = getConfigServiceAdminClient().getAllApplications();
        assertNotNull(applications);
        assertEquals(applications.size(), 1);
        Application application = applications.iterator().next();
        assertEquals(application.artifactId, this.application.artifactId);
        assertEquals(application.id, this.application.id);
    }


    //getApplicationStatus
    @Test(dependsOnMethods = "testRegisterApplicationOK")
    public void testApplicationStatus() throws Exception {
        getConfigServiceAdminClient().createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("arbitrary-config", application));

        // Only register client 1
        ClientConfig client1 = getConfigServiceClient().registerClient(new ClientRegistrationRequest(application.artifactId, "client-1-name"));

        // Register and update client 2
        ClientConfig registerClientResponse = getConfigServiceClient().registerClient(new ClientRegistrationRequest(application.artifactId, "client-2-name"));
        ClientConfig client2 = getConfigServiceClient().checkForUpdate(registerClientResponse.clientId, new CheckForUpdateRequest("force-updated-config"));

        ApplicationStatus applicationStatus = getConfigServiceAdminClient().getApplicationStatus(application.artifactId);

        assertEquals(applicationStatus.numberOfRegisteredClients, Integer.valueOf(2));
        assertEquals(applicationStatus.seenInTheLastHourCount, Integer.valueOf(2));
        assertNotNull(applicationStatus.seenInTheLastHour);
        assertEquals(applicationStatus.seenInTheLastHour.size(), 2);
        assertTrue(applicationStatus.seenInTheLastHour.contains(client1.clientId));
        assertTrue(applicationStatus.seenInTheLastHour.contains(client2.clientId));
        assertNotNull(applicationStatus.allClientHeartbeatData);
        assertNotNull(applicationStatus.allClientHeartbeatData.get(client1.clientId));
        assertNotNull(applicationStatus.allClientHeartbeatData.get(client2.clientId));
    }
}
