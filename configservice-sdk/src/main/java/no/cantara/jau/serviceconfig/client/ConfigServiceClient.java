package no.cantara.jau.serviceconfig.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfigSerializer;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class ConfigServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ConfigServiceClient.class);

    public static String fetchServiceConfig(String url, String username, String password) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        if (username != null && password != null) {
            String usernameAndPassword = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encoded);
        }
        conn.setDoOutput(true);

        try (Reader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            StringBuilder result = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                result.append((char) c);
            }
            return result.toString();
        }
    }

    public static ServiceConfig fetchAndParseServiceConfig(String url, String username, String password)
            throws IOException {

        String response = fetchServiceConfig(url, username, password);
        log.debug("Fetched ServiceConfig (length: {}).", response.length());

        //Parse
        ServiceConfig serviceConfig = ServiceConfigSerializer.fromJson(response);
        log.debug("Parsed serviceConfig (timestamp: {})", serviceConfig.getChangedTimestamp());
        return serviceConfig;
    }
}
