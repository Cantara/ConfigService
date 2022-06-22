package no.cantara.cs.testsupport;

import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.client.ConfigServiceClient;
import org.testng.annotations.*;

/**
 * Configures a test server with either embedded or postgres persistence
 *
 * @author Anders Emil Rønning
 */

@Test(groups = "system-test")
public abstract class BaseSystemTest {
    private TestServer testServer;
    private ConfigServiceClient configServiceClient;

    @BeforeClass
    @Parameters({ "persistenceType" })
    public void startServer(@Optional("embedded") String persistenceType) throws Exception {
        if (persistenceType.equals("postgres")) {
            testServer = new TestServerPostgres(getClass());
            //System.out.println("Starting external postgres\n\n\n\n\n\n\n\n\n");
        } else {
            testServer = new TestServerEmbedded(getClass());
            //System.out.println("Starting embedded postgres\n\n\n\n\n\n\n\n\n");
        }
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

    public TestServer getTestServer() {
        return testServer;
    }

    public ConfigServiceClient getConfigServiceClient() {
        return configServiceClient;
    }

    public ConfigServiceAdminClient getConfigServiceAdminClient() {
        return testServer.getAdminClient();
    }
}
