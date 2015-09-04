package no.cantara.jau.serviceconfig.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Responsible for serializing ClientConfig to/from json and xml.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
@Deprecated
public class ClientConfigSerializer {
    private static final Logger log = LoggerFactory.getLogger(ClientConfigSerializer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(ClientConfig clientConfig) {
        String createdJson = null;
        try {
            createdJson = mapper.writeValueAsString(clientConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", clientConfig.toString());
        }
        return createdJson;
    }

    //list of serviceConfig data, no wrapping element. Need to decide.
    public static String toJson(List<ClientConfig> clientConfigs) {
        String createdJson = null;
        try {
            createdJson = mapper.writeValueAsString(clientConfigs);
        } catch (IOException e) {
            log.warn("Could not convert to Json.");
        }
        return createdJson;
    }


    //Should probably use JsonPath
    public static ClientConfig fromJson(String json) {
        try {
            ClientConfig clientConfig = mapper.readValue(json, ClientConfig.class);
            return clientConfig;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error mapping json for " + json, e);
        }
    }

    public static List<ClientConfig> fromJsonList(String json) {
        try {
            List<ClientConfig> clientConfigs = mapper.readValue(json, new TypeReference<List<ClientConfig>>() { });
            return clientConfigs;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error mapping json for " + json, e);
        }
    }
}
