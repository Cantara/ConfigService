package no.cantara.cs.client;

import com.jayway.restassured.http.ContentType;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.testsupport.TestServer;
import org.apache.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class CheckForUpdateTest {

    private ConfigServiceClient configServiceClient;

    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
        configServiceClient.cleanApplicationState();
    }

    @Test
    public void testCheckForUpdateBrokenJson() {
        given()
                .auth().basic(TestServer.USERNAME, TestServer.PASSWORD)
                .contentType(ContentType.JSON)
                .body("{broken json}")
                .log().everything()
                .expect()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .log().ifError()
                .when()
                .post(ClientResource.CLIENT_PATH + "/{clientId}/sync", 1);
    }

    @Test
    public void testCheckForUpdateUnregisteredClient() throws IOException {
        try {
            configServiceClient.checkForUpdate("non-existing-client-id", new CheckForUpdateRequest(""));
            fail();
        } catch (HttpException e) {
            assertEquals(e.getStatusCode(), HttpStatus.SC_PRECONDITION_FAILED);
        }
    }

    @Test
    public void testCheckForUpdateRequestParsingBackwardCompatible() throws Exception {
        /*
        CheckForUpdateRequest request = new CheckForUpdateRequest("configLastChanged");
        Map<String, String> envInfo = new HashMap<>();
        envInfo.put("envKey1", "envValue1");
        envInfo.put("envKey2", "envValue2");
        request.envInfo = envInfo;
        request.tags = "tagA, tabB, tagC";
        request.clientName = "clientName123";
        //request.eventsStore = new ExtractedEventsStore();
        //request.eventsStore.addEvents(Collections.singletonList(new Event(43, "this is a log statement")));

        String jsonResult = new ObjectMapper().writeValueAsString(request);
        */
        String configLastChanged1 = "configLastChanged";
        String json1 = "{\n" +
                "  \"configLastChanged\": \"" + configLastChanged1 + "\",\n" +
                "  \"envInfo\": {\n" +
                "    \"envKey1\": \"envValue1\",\n" +
                "    \"envKey2\": \"envValue2\"\n" +
                "  },\n" +
                "  \"tags\": \"tagA, tabB, tagC\",\n" +
                "  \"clientName\": \"clientName123\",\n" +
                "  \"eventsStore\": null\n" +
                "}";
        CheckForUpdateRequest checkForUpdateRequest = ClientResource.fromJson(json1);
        assertEquals(checkForUpdateRequest.configLastChanged, configLastChanged1);

        String clientId2 = "clientId2";
        String clientSecret2 = "clientSecret2";
        String configLastChanged2 = "configLastChanged2";
        String json2 = "{\n" +
                "  \"clientId\": \"" + clientId2 + "\",\n" +
                "  \"clientSecret\": \"" + clientSecret2 + "\",\n" +
                "  \"configLastChanged\": \"" + configLastChanged2 + "\",\n" +
                "  \"envInfo\": {\n" +
                "    \"envKey1\": \"envValue1\",\n" +
                "    \"envKey2\": \"envValue2\"\n" +
                "  },\n" +
                "  \"tags\": \"tagA, tabB, tagC\",\n" +
                "  \"clientName\": \"clientName123\",\n" +
                "  \"eventsStore\": null\n" +
                "}";
        CheckForUpdateRequest checkForUpdateRequest2 = ClientResource.fromJson(json2);
        assertEquals(checkForUpdateRequest2.clientId, clientId2);
        assertEquals(checkForUpdateRequest2.clientSecret, clientSecret2);
        assertEquals(checkForUpdateRequest2.configLastChanged, configLastChanged2);
    }
}
