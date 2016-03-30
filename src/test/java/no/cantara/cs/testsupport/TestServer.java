package no.cantara.cs.testsupport;

import com.jayway.restassured.RestAssured;
import no.cantara.cs.Main;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static java.util.Arrays.stream;

public class TestServer {

    private static final Logger log = LoggerFactory.getLogger(TestServer.class);

    public static final String MAPDB_FOLDER = "./db/test";
    public static final String USERNAME = "read";
    public static final String PASSWORD = "baretillesing";

    private Main main;
    private String url;
    private String mapDbName;
    private Class testClass;

    public TestServer(Class testClass) {
        this.testClass = testClass;
        mapDbName = testClass.getSimpleName() + ".db";
    }

    public void cleanAllData() throws Exception {
        File mapDbFolder = new File(MAPDB_FOLDER);
        if (mapDbFolder.exists()) {
            stream(mapDbFolder.listFiles((dir, name) -> {
                return name.startsWith(mapDbName);
            })).forEach(f -> {
                log.info("Deleting mapdb file: " + f.getAbsolutePath());
                f.delete();
            });
        }
    }

    public void start() throws InterruptedException {
        String mapDbPath = MAPDB_FOLDER + "/" + mapDbName;
        new Thread(() -> {
            main = new Main(mapDbPath);
            main.start();
        }).start();
        do {
            Thread.sleep(10);
        } while (main == null || !main.isStarted());
        RestAssured.port = main.getPort();

        RestAssured.basePath = Main.CONTEXT_PATH;
        url = "http://localhost:" + main.getPort() + Main.CONTEXT_PATH + ClientResource.CLIENT_PATH;
    }

    public void stop() {
        main.stop();
    }

    public ConfigServiceClient getConfigServiceClient() {
        return new ConfigServiceClient(url, USERNAME, PASSWORD).withApplicationStateFilename(MAPDB_FOLDER + "/" + testClass.getSimpleName() + ".properties");
    }

    public ConfigServiceAdminClient getAdminClient() {
        return new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
    }
}
