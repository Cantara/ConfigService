package no.cantara.jau.serviceconfig.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-04.
 */
public class ClientRegistration {
    public String artifactId;
    public Map<String, String> envInfo;

    public ClientRegistration() {
        this.envInfo = new HashMap<>();
    }

    public ClientRegistration(String artifactId) {
        this.artifactId = artifactId;
        this.envInfo = new HashMap<>();
    }
}
