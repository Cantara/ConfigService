package no.cantara.jau.serviceconfig.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.serviceconfig.dto.*;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class ConfigServiceClient {
    public static final Charset CHARSET = Charset.forName("UTF-8");
    public static final String APPLICATION_STATE_FILENAME = "applicationState.properties";
    public static final String CLIENT_ID = "clientId";
    public static final String LAST_CHANGED = "lastChanged";
    public static final String COMMAND = "command";
    public static final String EVENT_EXTRACTION_CONFIGS = "eventExtractionConfigs";
    private static final Logger log = LoggerFactory.getLogger(ConfigServiceClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String url;
    private final String username;
    private final String password;


    public ConfigServiceClient(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public ClientConfig registerClient(ClientRegistrationRequest request) throws IOException {
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

        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();

        if (responseCode != HttpURLConnection.HTTP_OK) {
            ClientResponseErrorHandler.handle(responseCode, responseMessage, url);
        }

        try (Reader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
            StringBuilder result = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                result.append((char) c);
            }
            String jsonResponse = result.toString();
            ClientConfig clientConfig = mapper.readValue(jsonResponse, ClientConfig.class);
            log.info("registerClient ok. clientId={}", clientConfig.clientId);
            return clientConfig;
        }
    }


    public void saveApplicationState(ClientConfig clientConfig) {
        final Properties applicationState = new Properties();
        applicationState.put(CLIENT_ID, clientConfig.clientId);
        applicationState.put(LAST_CHANGED, clientConfig.serviceConfig.getLastChanged());
        applicationState.put(COMMAND, clientConfig.serviceConfig.getStartServiceScript());

        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonEventExtractionTags = mapper.writeValueAsString(clientConfig.serviceConfig
                    .getEventExtractionConfigs());
            applicationState.put(EVENT_EXTRACTION_CONFIGS, jsonEventExtractionTags);
        } catch (JsonProcessingException io) {
            throw new RuntimeException(io);
        }
        OutputStream output = null;
        try {
            output = new FileOutputStream(APPLICATION_STATE_FILENAME);
            // save properties to project root folder
            applicationState.store(output, null);
        } catch (IOException io) {
            throw new RuntimeException(io);
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    //intentionally ignored
                }
            }
        }
    }
    public Properties getApplicationState() {
        if (!new File(APPLICATION_STATE_FILENAME).exists()) {
            return null;
        }

        Properties properties = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(APPLICATION_STATE_FILENAME);
            properties.load(input);
            return properties;
        } catch (IOException io) {
            throw new RuntimeException(io);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    //intentionally ignored
                }
            }
        }
    }

    public List<EventExtractionConfig> getEventExtractionConfigs() {
        String eventExtractionConfigs = getApplicationState().getProperty(EVENT_EXTRACTION_CONFIGS);
        if (eventExtractionConfigs == null) {
            return new ArrayList<>();
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(eventExtractionConfigs, new TypeReference<List<EventExtractionConfig>>(){});
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    public void cleanApplicationState() {
        File applicationStatefile = new File(APPLICATION_STATE_FILENAME);
        if (applicationStatefile.exists()) {
            applicationStatefile.delete();
        }
    }


    public ClientConfig checkForUpdate(String clientId, CheckForUpdateRequest checkForUpdateRequest) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url + "/" + clientId).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        if (username != null && password != null) {
            String usernameAndPassword = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoded);
        }

        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        String jsonRequest = mapper.writeValueAsString(checkForUpdateRequest);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(jsonRequest.getBytes(CHARSET));
        }

        String responseMessage = connection.getResponseMessage();
        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            ClientResponseErrorHandler.handle(responseCode, responseMessage, url);
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

    public String getUrl() {
        return url;
    }

}
