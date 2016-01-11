package no.cantara.jau.serviceconfig.dto;

import java.io.Serializable;

public class EventExtractionItem implements Serializable {
    private static final long serialVersionUID = 5585544430751810720L;

    public String regex;
    public String filePath;

    //for jackson
    private EventExtractionItem() {
    }

    public EventExtractionItem(String regex, String filePath) {
        this.regex = regex;
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return "eventExtractionItem{" +
                "regex='" + regex + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
