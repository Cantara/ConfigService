package no.cantara.cs.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Simple health endpoint for checking the server is running
 *
 * @author <a href="mailto:asbjornwillersrud@gmail.com">Asbjørn Willersrud</a> 30/03/2016.
 */
@Path(HealthResource.HEALTH_PATH)
public class HealthResource {
    public static final String HEALTH_PATH = "/health";
    private static final Logger log = LoggerFactory.getLogger(HealthResource.class);
    private final static String MAVEN_ARTIFACT_ID = "configservice";
    private static String myIp;
    private static String myVersion;

    @GET
    @Produces("application/json")
    public Response healthCheck() {
        String json = "{" +
                "\"service\":\"" + MAVEN_ARTIFACT_ID
                + "\",\"Status\":\"" + "OK"
                + "\",\"timestamp\":\"" + Instant.now().toString()
                + "\",\"IP\":\"" + getMyIPAddresssString()
                + "\",\"runningSince\":\"" + getRunningSince()
                + "\",\"version\":\"" + getVersion()
                + "\"}";
        return Response.ok(json).build();
    }

    private String getRunningSince() {
        long uptimeInMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        return Instant.now().minus(uptimeInMillis, ChronoUnit.MILLIS).toString();
    }


    private static String getVersion() {
        Properties mavenProperties = new Properties();
        String resourcePath = "/META-INF/maven/no.cantara.jau/configservice/pom.properties";

        URL mavenVersionResource = HealthResource.class.getResource(resourcePath);
        if (mavenVersionResource != null) {
            try {
                mavenProperties.load(mavenVersionResource.openStream());
                return mavenProperties.getProperty("version", "missing version info in " + resourcePath);
            } catch (IOException e) {
                log.warn("Problem reading version resource from classpath: ", e);
            }
        }
        return "(DEV VERSION)" + getMyIPAddresssesString() + "]";
//        return myVersion;

    }

    public static String getMyIPAddresssesString() {
        String ipAdresses = "";

        try {
            ipAdresses = InetAddress.getLocalHost().getHostAddress();
            Enumeration n = NetworkInterface.getNetworkInterfaces();

            while (n.hasMoreElements()) {
                NetworkInterface e = (NetworkInterface) n.nextElement();

                InetAddress addr;
                for (Enumeration a = e.getInetAddresses(); a.hasMoreElements(); ipAdresses = ipAdresses + "  " + addr.getHostAddress()) {
                    addr = (InetAddress) a.nextElement();
                }
            }
        } catch (Exception e) {
            ipAdresses = "Not resolved";
        }

        return ipAdresses;
    }

    public static String getMyIPAddresssString() {
        if (myIp == null || myIp.length() < 1) {
            String fullString = getMyIPAddresssesString();
            myIp = fullString.substring(0, fullString.indexOf(" "));
        }
        return myIp;
    }
}
