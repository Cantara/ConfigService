package no.cantara.jau.serviceconfig.dto.event;

import java.util.LinkedList;
import java.util.List;

public class EventTag {
    private List<String> logEvent;

    public EventTag() {
        this.logEvent = new LinkedList<>();
    }

    public List<String> getEvents() {
        return logEvent;
    }

    public void addEvents(List<Event> events) {
        events.stream().forEach(event -> logEvent.add(event.getLine()));
    }

}
