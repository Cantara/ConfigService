package no.cantara.cs.client;

import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientRegistrationRequest;

import java.util.Date;
import java.util.Map;

/**
 * Created by jorunfa on 04/11/15.
 */
public class ClientStatus {

    public String artifactId;
    public Map<String, String> envInfo;
    public String tags;
    public String clientName;
    public String configLastChanged;
    public String timeOfContact;

    public ClientStatus(ClientRegistrationRequest registration) {
        this.artifactId = registration.artifactId;
        this.clientName = registration.clientName;
        this.envInfo = registration.envInfo;
        this.tags = registration.tags;
        this.configLastChanged = "Never changed.";
        this.timeOfContact = new Date().toInstant().toString();
    }

    public ClientStatus(CheckForUpdateRequest checkForUpdateRequest, String artifactId) {
        this.clientName = checkForUpdateRequest.clientName;
        this.tags = checkForUpdateRequest.tags;
        this.envInfo = checkForUpdateRequest.envInfo;
        this.configLastChanged = checkForUpdateRequest.configLastChanged;
        this.artifactId = artifactId;
        this.timeOfContact = new Date().toInstant().toString();
    }
}
