package no.cantara.cs.persistence;

import no.cantara.cs.dto.ClientEnvironment;
import no.cantara.cs.dto.ClientHeartbeatData;
import no.cantara.cs.dto.Client;

import java.util.Map;

public interface ClientDao {

    Client getClient(String clientId);

    void saveClient(Client client);

    void saveClientHeartbeatData(String clientId, ClientHeartbeatData clientHeartbeatData);

    ClientHeartbeatData getClientHeartbeatData(String clientId);

    void saveClientEnvironment(String clientId, ClientEnvironment clientEnvironment);

    ClientEnvironment getClientEnvironment(String clientId);

    Map<String,ClientHeartbeatData> getAllClientHeartbeatData(String artifactId);
}
