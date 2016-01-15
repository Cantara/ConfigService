package no.cantara.jau.serviceconfig.client;

import no.cantara.jau.serviceconfig.dto.event.Event;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionTag;
import no.cantara.jau.serviceconfig.dto.event.ExtractedEventsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class EventExtractionUtil {
    private static final Logger log = LoggerFactory.getLogger(EventExtractionUtil.class);

    public static Map<String, List<EventExtractionTag>> groupExtractionConfigsByFile(
            EventExtractionConfig config) {
        Map<String, List<EventExtractionTag>> collect = config.tags.stream()
                .collect(groupingBy(item -> item.filePath));
        log.info(collect.toString());
        return collect;
    }

    public static ExtractedEventsStore mapToExtractedEvents(List<Event> events) {
        ExtractedEventsStore mappedEvents = new ExtractedEventsStore();
        mappedEvents.addEvents(events);
        return mappedEvents;
    }
}
