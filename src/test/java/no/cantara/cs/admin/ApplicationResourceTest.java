package no.cantara.cs.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.http.ContentType;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.dto.*;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.*;

/**
 * @author Asbjørn Willersrud 2016-03-10
 */
public class ApplicationResourceTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private TestServer testServer;
    private ConfigServiceAdminClient configServiceAdminClient;
    private Application application;

    @BeforeClass
    public void setup() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();

        configServiceAdminClient = testServer.getAdminClient();
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
    }


    //createApplication
    @Test
    public void testRegisterApplicationOK() throws IOException {
        application = configServiceAdminClient.registerApplication(getClass().getSimpleName());
    }

    @Test(dependsOnMethods = "testRegisterApplicationOK")
    public void testRegisterApplicationWithDuplicateArtifactIdShouldReturnBadRequest() throws IOException {
        Application application1 = new Application(getClass().getSimpleName());
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


    //getAllApplications
    @Test(dependsOnMethods = "testRegisterApplicationOK")
    public void testGetApplications() throws IOException {
        List<Application> applications = testServer.getAdminClient().getAllApplications();
        assertNotNull(applications);
        assertEquals(applications.size(), 1);
        Application application = applications.iterator().next();
        assertEquals(application.artifactId, this.application.artifactId);
        assertEquals(application.id, this.application.id);
    }


    //getApplicationStatus
    @Test(dependsOnMethods = "testRegisterApplicationOK")
    public void testApplicationStatus() throws Exception {
        configServiceAdminClient.createApplicationConfig(application, ApplicationConfigBuilder.createConfigDto("arbitrary-config", application));

        // Only register client 1
        ClientConfig client1 = testServer.getConfigServiceClient().registerClient(new ClientRegistrationRequest(application.artifactId, "client-1-name"));

        // Register and update client 2
        ClientConfig registerClientResponse = testServer.getConfigServiceClient().registerClient(new ClientRegistrationRequest(application.artifactId, "client-2-name"));
        ClientConfig client2 = testServer.getConfigServiceClient().checkForUpdate(registerClientResponse.clientId, new CheckForUpdateRequest("force-updated-config"));

        ApplicationStatus applicationStatus = testServer.getAdminClient().getApplicationStatus(application.artifactId);

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
