package no.cantara.jau.serviceconfig.dto.event;

import java.io.Serializable;

public class EventExtractionTag implements Serializable {
    private static final long serialVersionUID = 5585544430751810720L;

    public String tagName;
    public String regex;
    public String filePath;

    //for jackson
    private EventExtractionTag() {
    }

    public EventExtractionTag(String tagName, String regex, String filePath) {
        this.tagName = tagName;
        this.regex = regex;
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "eventExtractionTag{" +
                "tagName='" + tagName + '\'' +
                "regex='" + regex + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
