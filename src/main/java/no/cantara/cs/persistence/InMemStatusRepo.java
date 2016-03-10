package no.cantara.cs.persistence;

import no.cantara.cs.client.ClientStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jorunfa on 04/11/15.
 */
@Service
public class InMemStatusRepo implements StatusDao {

    private Map<String, ClientStatus> statuses;

    public InMemStatusRepo() {
        this.statuses = new HashMap<>();
    }

    @Override
    public void saveStatus(String clientId, ClientStatus clientStatus) {
        statuses.put(clientId, clientStatus);
    }

    @Override
    public Map<String, ClientStatus> getAllStatuses() {
        return statuses;
    }

    @Override
    public ClientStatus getStatus(String clientId) {
        return statuses.get(clientId);
    }

    @Override
    public Map<String, ClientStatus> getAllStatuses(String artifactId) {
        return statuses.entrySet()
                .stream()
                .filter(entry -> entry.getValue().artifactId.equals(artifactId))
                .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }
}
