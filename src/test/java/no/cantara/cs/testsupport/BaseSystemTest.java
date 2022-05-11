package no.cantara.cs.testsupport;

import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.client.ConfigServiceClient;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

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
        } else {
            testServer = new TestServerEmbedded(getClass());
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
