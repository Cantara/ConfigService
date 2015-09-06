package no.cantara.jau.serviceconfig.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-04.
 */
public class CheckForUpdateRequest {
    public String checksum;
    public Map<String, String> envInfo;

    //jackson
    private CheckForUpdateRequest() {
    }

    public CheckForUpdateRequest(String checksum) {
        this.checksum = checksum;
        this.envInfo = new HashMap<>();
    }

    public CheckForUpdateRequest(String checksum, Map<String, String> envInfo) {
        this.checksum = checksum;
        this.envInfo = envInfo;
    }
}
