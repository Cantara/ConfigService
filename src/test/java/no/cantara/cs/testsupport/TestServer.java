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

    private Main main;
    private String url;
    private int port = 26451;

    public static final String USERNAME = "read";
    public static final String PASSWORD = "baretillesing";

    public TestServer withPort(int port) {
        this.port = port;
        return this;
    }

    public void cleanAllData() throws Exception {
        File mapDbFolder = new File("./db");
        stream(mapDbFolder.listFiles((dir, name) -> name.startsWith("serviceConfig.db"))).forEach(f -> {
            log.info("Deleting mapdb file: " + f.getAbsolutePath());
            f.delete();
        });
    }

    public void start() throws InterruptedException {
        new Thread(() -> {
            main = new Main(port);
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
        return new ConfigServiceClient(url, USERNAME, PASSWORD);
    }

    public ConfigServiceAdminClient getAdminClient() {
        return new ConfigServiceAdminClient(TestServer.USERNAME, TestServer.PASSWORD);
    }
}
