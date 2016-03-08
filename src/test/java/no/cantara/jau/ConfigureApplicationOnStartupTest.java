package no.cantara.jau;

import no.cantara.jau.serviceconfig.client.ApplicationConfigurator;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.Application;
import no.cantara.jau.serviceconfig.dto.NamedPropertiesStore;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.testsupport.ConfigServiceAdminClient;
import no.cantara.jau.testsupport.ServiceConfigBuilder;
import no.cantara.jau.testsupport.TestServer;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.util.Map;
import java.util.Properties;

import static org.testng.Assert.*;

/**
 * Verify endpoints used by when applications contact ConfigService directly (no JAU).
 */
public class ConfigureApplicationOnStartupTest {
    private ConfigServiceClient configServiceClient;
    private Application testApplication;
    private ServiceConfig currentServiceConfig;
    private ConfigServiceAdminClient configServiceAdminClient;
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer();
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        testApplication = configServiceAdminClient.registerApplication("ClientConfigResourceTestApplication");
        currentServiceConfig = configServiceAdminClient.registerServiceConfig(testApplication, ServiceConfigBuilder.createServiceConfigDto("first", testApplication));
    }

    @AfterClass
    public void stop() {
        if (testServer != null) {
            testServer.stop();
        }
        configServiceClient.cleanApplicationState();
    }

    @Test
    public void testConfigureApplicationOnStartup() throws Exception {
        File applicationStateFile = new File(ConfigServiceClient.APPLICATION_STATE_FILENAME);
        assertFalse(applicationStateFile.exists(), "Test precondition is that applicationState is not persisted");

        File configurationStoreDirectory = new File("target/test-config-directory");

        ApplicationConfigurator applicationConfigurator = new ApplicationConfigurator(configServiceClient);
        applicationConfigurator.configureApplication(testApplication.artifactId, configurationStoreDirectory.getAbsolutePath());

        assertTrue(applicationStateFile.exists());

        for (NamedPropertiesStore namedPropertiesStore : this.currentServiceConfig.getConfigurationStores()) {
            File propertiesFile = new File(configurationStoreDirectory + File.separator + namedPropertiesStore.fileName);
            assertTrue(propertiesFile.exists());

            Properties properties = new Properties();
            properties.load(new FileReader(propertiesFile));
            for (Map.Entry<String, String> property : namedPropertiesStore.properties.entrySet()) {
                assertEquals(properties.getProperty(property.getKey()), property.getValue());
            }
        }

        // Test restart
        applicationConfigurator.configureApplication(testApplication.artifactId, configurationStoreDirectory.getAbsolutePath());
    }
}