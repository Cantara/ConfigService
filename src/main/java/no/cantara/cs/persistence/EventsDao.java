package no.cantara.cs.persistence;

import no.cantara.cs.dto.event.ExtractedEventsStore;
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
