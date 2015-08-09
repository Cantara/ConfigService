package no.cantara.jau.serviceconfig.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class ConfigServiceClient {
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
                result.append((char)c);
            }
            return result.toString();
        }
    }
}
