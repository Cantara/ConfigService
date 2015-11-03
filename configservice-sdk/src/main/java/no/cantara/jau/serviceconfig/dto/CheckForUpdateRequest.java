package no.cantara.jau.serviceconfig.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-04.
 */
public class CheckForUpdateRequest {
    public String serviceConfigLastChanged;
    public Map<String, String> envInfo;
    public String clientName;

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
}
