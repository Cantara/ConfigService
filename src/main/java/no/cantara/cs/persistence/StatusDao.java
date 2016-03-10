package no.cantara.cs.persistence;

import no.cantara.cs.client.ClientStatus;

import java.util.Map;

/**
 * Created by jorunfa on 04/11/15.
 */
public interface StatusDao {
    void saveStatus(String clientId, ClientStatus clientStatus);
    Map<String, ClientStatus> getAllStatuses();
    ClientStatus getStatus(String clientId);

    Map<String,ClientStatus> getAllStatuses(String artifactId);
}
