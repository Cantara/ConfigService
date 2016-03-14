package no.cantara.cs.testsupport.dto;

import no.cantara.cs.client.ClientStatus;

import java.util.List;
import java.util.Map;

public class ApplicationStatus {

    public Integer numberOfRegisteredClients;
    public Integer seenInTheLastHourCount;
    public List<String> seenInTheLastHour;
    public Map<String, ClientStatus> allClientsSnapshot;

}
