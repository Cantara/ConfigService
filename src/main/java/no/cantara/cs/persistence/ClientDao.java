package no.cantara.cs.persistence;

import no.cantara.cs.dto.ClientEnvironment;
import no.cantara.cs.dto.ClientHeartbeatData;
import no.cantara.cs.dto.Client;
import no.cantara.cs.dto.ClientAlias;

import java.util.List;
import java.util.Map;

public interface ClientDao {

    Client getClient(String clientId);
    
    ClientHeartbeatData getClientHeartbeatData(String clientId);
    
    ClientEnvironment getClientEnvironment(String clientId);

    void saveClient(Client client);
    
    void saveClients(Client[] client);

    void saveClientHeartbeatData(String clientId, ClientHeartbeatData clientHeartbeatData); 

    void saveClientEnvironment(String clientId, ClientEnvironment clientEnvironment);

    void saveClientAlias(ClientAlias clientAlias);
    
    void saveIgnoredFlag(String clientId, boolean ignored);

    Map<String,ClientHeartbeatData> getAllClientHeartbeatData(String artifactId);

    List<Client> getAllClients();
    
    List<String> getAllIgnoredClientIds();
    
    List<ClientAlias> getAllClientAliases();

    Map<String, ClientHeartbeatData> getAllClientHeartbeatData();
    
    Map<String, ClientEnvironment> getAllClientEnvironments();
    
    List<Client> getAllClientsByConfigId(String configId);
    
}
