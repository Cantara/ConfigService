package no.cantara.jau.serviceconfig.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Responsible for serializing ServiceConfig to/from json and xml.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class ServiceConfigSerializer {
    private static final Logger log = LoggerFactory.getLogger(ServiceConfigSerializer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String toJson(ServiceConfig serviceConfig) {
        String createdJson = null;
        try {
            createdJson = mapper.writeValueAsString(serviceConfig);
        } catch (IOException e) {
            log.warn("Could not convert to Json {}", serviceConfig.toString());
        }
        return createdJson;
    }

    //list of serviceConfig data, no wrapping element. Need to decide.
    public static String toJson(List<ServiceConfig> applications) {
        String createdJson = null;
        try {
            createdJson = mapper.writeValueAsString(applications);
        } catch (IOException e) {
            log.warn("Could not convert to Json.");
        }
        return createdJson;
    }


    //Should probably use JsonPath
    public static ServiceConfig fromJson(String json) {
        try {
            ServiceConfig serviceConfig = mapper.readValue(json, ServiceConfig.class);
            return serviceConfig;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error mapping json for " + json, e);
        }
    }

    public static List<ServiceConfig> fromJsonList(String json) {
        try {
            List<ServiceConfig> serviceConfigs = mapper.readValue(json, new TypeReference<List<ServiceConfig>>() { });
            return serviceConfigs;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error mapping json for " + json, e);
        }
    }
}
