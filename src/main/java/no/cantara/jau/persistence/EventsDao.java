package no.cantara.jau.persistence;

import no.cantara.jau.clientconfig.ClientStatus;
import no.cantara.jau.serviceconfig.dto.event.ExtractedEventsStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EventsDao {
    private Map<String, ExtractedEventsStore> events;

    public EventsDao() {
        this.events = new HashMap<>();
    }

    public void saveEvents(String clientId, ExtractedEventsStore eventsStore) {
        events.put(clientId, eventsStore);
    }

    public Map<String, ExtractedEventsStore> getAllEvents() {
        return events;
    }

    public ExtractedEventsStore getEvents(String clientId) {
        return events.get(clientId);

    }
}
