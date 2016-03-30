package no.cantara.cs.client;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.NamedPropertiesStore;
import no.cantara.cs.testsupport.ApplicationConfigBuilder;
import no.cantara.cs.testsupport.ConfigServiceAdminClient;
import no.cantara.cs.testsupport.TestServer;
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
    private ApplicationConfig currentConfig;
    private TestServer testServer;

    @BeforeClass
    public void startServer() throws Exception {
        testServer = new TestServer(getClass());
        testServer.cleanAllData();
        testServer.start();
        configServiceClient = testServer.getConfigServiceClient();

        ConfigServiceAdminClient configServiceAdminClient = new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
        testApplication = configServiceAdminClient.registerApplication("ClientConfigResourceTestApplication");
        currentConfig = configServiceAdminClient.createApplicationConfig(testApplication, ApplicationConfigBuilder.createConfigDto("first", testApplication));
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
        File applicationStateFile = new File(configServiceClient.getApplicationStateFilename());
        assertFalse(applicationStateFile.exists(), "Test precondition is that applicationState is not persisted");

        File configurationStoreDirectory = new File("target/test-config-directory");

        ApplicationConfigurator applicationConfigurator = new ApplicationConfigurator(configServiceClient)
                .setArtifactId(testApplication.artifactId)
                .setConfigurationStoreDirectory(configurationStoreDirectory.getAbsolutePath());
        applicationConfigurator.configureApplication();

        assertTrue(applicationStateFile.exists());

        for (NamedPropertiesStore namedPropertiesStore : this.currentConfig.getConfigurationStores()) {
            File propertiesFile = new File(configurationStoreDirectory + File.separator + namedPropertiesStore.fileName);
            assertTrue(propertiesFile.exists());

            Properties properties = new Properties();
            properties.load(new FileReader(propertiesFile));
            for (Map.Entry<String, String> property : namedPropertiesStore.properties.entrySet()) {
                assertEquals(properties.getProperty(property.getKey()), property.getValue());
            }
        }

        // Test restart
        applicationConfigurator.configureApplication();
    }
}