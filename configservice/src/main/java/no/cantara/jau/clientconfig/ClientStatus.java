package no.cantara.jau.clientconfig;

import no.cantara.jau.serviceconfig.dto.CheckForUpdateRequest;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;

import java.time.Instant;
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
    public String serviceConfigLastChanged;
    public String timeOfContact;

    public ClientStatus(ClientRegistrationRequest registration) {
        this.artifactId = registration.artifactId;
        this.clientName = registration.clientName;
        this.envInfo = registration.envInfo;
        this.tags = registration.tags;
        this.serviceConfigLastChanged = "Never changed.";
        this.timeOfContact = new Date().toInstant().toString();
    }

    public ClientStatus(CheckForUpdateRequest checkForUpdateRequest, String artifactId) {
        this.clientName = checkForUpdateRequest.clientName;
        this.tags = checkForUpdateRequest.tags;
        this.envInfo = checkForUpdateRequest.envInfo;
        this.serviceConfigLastChanged = checkForUpdateRequest.serviceConfigLastChanged;
        this.artifactId = artifactId;
        this.timeOfContact = new Date().toInstant().toString();
    }
}
