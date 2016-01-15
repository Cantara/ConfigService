package no.cantara.jau.serviceconfig.dto.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EventExtractionConfig implements Serializable {
    private static final long serialVersionUID = 5987204485844627533L;

    public String groupName;
    public List<EventExtractionTag> tags;

    //for jackson
    private EventExtractionConfig() {
    }

    public EventExtractionConfig(String groupName) {
        this.groupName = groupName;
        this.tags = new ArrayList<>();
    }

    public void addEventExtractionTag(EventExtractionTag tag) {
        tags.add(tag);
    }

    @Override
    public String toString() {
        return "eventExtractionTag{" +
                "groupName='" + groupName + '\'' +
                "tags='" + tags + '\'' +
                '}';
    }

}
