package no.cantara.jau.serviceconfig.dto;

import java.io.Serializable;

public class EventExtractionConfig implements Serializable {
    public String regex;
    public String filePath;

    //for jackson
    private EventExtractionConfig() {
    }

    public EventExtractionConfig(String regex, String filePath) {
        this.regex = regex;
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "eventExtractionConfig{" +
                "regex='" + regex + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
