package no.cantara.jau.serviceconfig.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.serviceconfig.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class ConfigServiceClient {
    private static final Logger log = LoggerFactory.getLogger(ConfigServiceClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    public static final Charset CHARSET = Charset.forName("UTF-8");


    public static ClientConfig registerClient(String url, String username, String password, ClientRegistrationRequest request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        if (username != null && password != null) {
            String usernameAndPassword = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoded);
        }

        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        String jsonRequest = mapper.writeValueAsString(request);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(jsonRequest.getBytes(CHARSET));
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED && connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            log.warn("registerClient failed. responseCode={}, responseMessage={}", connection.getResponseCode(), connection.getResponseMessage());
            return null;
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder result = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                result.append((char) c);
            }
            String jsonResponse = result.toString();
            return mapper.readValue(jsonResponse, ClientConfig.class);
        }
    }

    public static ClientConfig checkForUpdate(String url, String username, String password, String clientId, String configChecksum,
                                              Map<String, String> envInfo) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url + "/" + clientId).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        if (username != null && password != null) {
            String usernameAndPassword = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoded);
        }

        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        String jsonRequest = mapper.writeValueAsString(new CheckForUpdateRequest(configChecksum, envInfo));
        try (OutputStream output = connection.getOutputStream()) {
            output.write(jsonRequest.getBytes(CHARSET));
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
            return null;
        }

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            log.warn("checkForUpdate failed. responseCode={}, responseMessage={}", connection.getResponseCode(), connection.getResponseMessage());
            return null;
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder result = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                result.append((char) c);
            }
            String jsonResponse = result.toString();
            return mapper.readValue(jsonResponse, ClientConfig.class);
        }
    }


    @Deprecated
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

    @Deprecated
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
