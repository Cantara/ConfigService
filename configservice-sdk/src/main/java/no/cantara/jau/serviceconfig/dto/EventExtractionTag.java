package no.cantara.jau.serviceconfig.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EventExtractionTag implements Serializable {
    private static final long serialVersionUID = 5987204485844627533L;

    public String tagName;
    public List<EventExtractionItem> items;

    //for jackson
    private EventExtractionTag() {
    }

    public EventExtractionTag(String tagName) {
        this.tagName = tagName;
        this.items = new ArrayList<>();
    }

    public void addEventExtractionItem(EventExtractionItem item) {
        items.add(item);
    }

    @Override
    public String toString() {
        return "eventExtractionTag{" +
                "items='" + items + '\'' +
                '}';
    }
}
