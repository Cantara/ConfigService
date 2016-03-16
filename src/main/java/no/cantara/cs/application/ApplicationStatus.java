package no.cantara.cs.application;

import no.cantara.cs.dto.ClientHeartbeatData;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplicationStatus {

    public Integer numberOfRegisteredClients;
    public Integer seenInTheLastHourCount;
    public List<String> seenInTheLastHour;
    public Map<String, ClientHeartbeatData> allClientHeartbeatData;

    private ApplicationStatus() {
        // For Jackson
    }

    public ApplicationStatus(Map<String, ClientHeartbeatData> allClientHeartbeatData) {
        this.allClientHeartbeatData = allClientHeartbeatData;
        this.numberOfRegisteredClients = allClientHeartbeatData.size();

        Instant oneHourAgo = new Date().toInstant().minusSeconds(60 * 60);

        List<String> seenInTheLastHour = allClientHeartbeatData.entrySet().stream()
                .filter(e -> Instant.parse(e.getValue().timeOfContact).isAfter(oneHourAgo))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        this.seenInTheLastHourCount = seenInTheLastHour.size();
        this.seenInTheLastHour = seenInTheLastHour;
    }
}
