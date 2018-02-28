package no.cantara.cs.testsupport;

import com.jayway.restassured.RestAssured;
import no.cantara.cs.Main;
import no.cantara.cs.client.ClientResource;
import no.cantara.cs.client.ConfigServiceAdminClient;
import no.cantara.cs.client.ConfigServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static java.util.Arrays.stream;

public class TestServerMapDb implements TestServer {

    private static final Logger log = LoggerFactory.getLogger(TestServerPostgres.class);

    public static final String MAPDB_FOLDER = "./target/db/test";

    private Main main;
    private String url;
    private String mapDbName;
    private Class testClass;

    public TestServerMapDb(Class testClass) {
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
            main = new Main(mapDbPath, "mapdb");
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
        return new ConfigServiceAdminClient("http://localhost:" + main.getPort() + Main.CONTEXT_PATH, TestServerPostgres.ADMIN_USERNAME, TestServer.ADMIN_PASSWORD);
    }
}
