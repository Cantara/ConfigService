package no.cantara.jau.serviceconfig.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-04.
 */
public class ClientRegistrationRequest {
    public String artifactId;
    public Map<String, String> envInfo;
    public String tags;
    public String clientName;

    public ClientRegistrationRequest() {
        this.envInfo = new HashMap<>();
    }

    public ClientRegistrationRequest(String artifactId) {
        this.artifactId = artifactId;
        this.envInfo = new HashMap<>();
    }

    public ClientRegistrationRequest(String artifactId, String clientName) {
        this(artifactId);
        this.clientName = clientName;
    }
}
