package no.cantara.jau.serviceconfig.dto;

import no.cantara.jau.serviceconfig.dto.event.ExtractedEventsStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-04.
 */
public class CheckForUpdateRequest {
    public String serviceConfigLastChanged;
    public Map<String, String> envInfo;
    public String tags;
    public String clientName;
    public ExtractedEventsStore eventsStore;

    //jackson
    private CheckForUpdateRequest() {
    }

    public CheckForUpdateRequest(String serviceConfigLastChanged) {
        this.serviceConfigLastChanged = serviceConfigLastChanged;
        this.envInfo = new HashMap<>();
    }

    public CheckForUpdateRequest(String serviceConfigLastChanged, Map<String, String> envInfo) {
        this.serviceConfigLastChanged = serviceConfigLastChanged;
        this.envInfo = envInfo;
    }

    public CheckForUpdateRequest(String serviceConfigLastChanged, Map<String, String> envInfo, String clientName) {
        this(serviceConfigLastChanged, envInfo);
        this.clientName = clientName;
    }

    public CheckForUpdateRequest(String serviceConfigLastChanged, Map<String, String> envInfo, String clientName,
                                 ExtractedEventsStore eventsStore) {
        this(serviceConfigLastChanged, envInfo);
        this.clientName = clientName;
        this.eventsStore= eventsStore;
    }

    public CheckForUpdateRequest(String serviceConfigLastChanged, Map<String, String> envInfo, String tags,
                                 String clientName) {
        this(serviceConfigLastChanged, envInfo, clientName);
        this.tags = tags;
    }
}
