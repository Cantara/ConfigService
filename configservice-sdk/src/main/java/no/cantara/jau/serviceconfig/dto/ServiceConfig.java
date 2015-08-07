package no.cantara.jau.serviceconfig.dto;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class ServiceConfig {
    private static final DateTimeFormatter df = DateTimeFormatter.ISO_INSTANT;

    /**
     * Human readable name of this service config.
     * For example "artifactId-version" of executable jar file.
     */
    private String name;
    /**
     * A timestamp when the config was last changed.
     * Primarily intended that clients should choose to update their ServiceConfig if this timestamp is not identical to their previous timestamp.
     * Timestamp is chosen over hash for human readability reasons only.
     *
     * ISO 8601 combined format, see https://en.wikipedia.org/?title=ISO_8601#Combined_date_and_time_representations
     * For example "2007-04-05T14:30".
     */
    private String changedTimestamp;
    private List<DownloadItem> downloadItems;
    private List<NamedPropertiesStore> configurationStores;

    private String startServiceScript;

    //for jackson
    private ServiceConfig() {
    }

    public ServiceConfig(String name) {
        this.name = name;
        this.changedTimestamp = df.format(Instant.now());
        this.downloadItems = new ArrayList<>();
        this.configurationStores = new ArrayList<>();

    }

    public void addDownloadItem(DownloadItem downloadItem) {
        downloadItems.add(downloadItem);
    }

    public void addPropertiesStore(NamedPropertiesStore propertiesStore) {
        configurationStores.add(propertiesStore);
    }


    public void setName(String name) {
        this.name = name;
    }
    public void setChangedTimestamp(String changedTimestamp) {
        this.changedTimestamp = changedTimestamp;
    }
    public void setDownloadItems(List<DownloadItem> downloadItems) {
        this.downloadItems = downloadItems;
    }
    public void setStartServiceScript(String startServiceScript) {
        this.startServiceScript = startServiceScript;
    }

    public String getName() {
        return name;
    }
    public String getChangedTimestamp() {
        return changedTimestamp;
    }
    public List<DownloadItem> getDownloadItems() {
        return downloadItems;
    }
    public List<NamedPropertiesStore> getConfigurationStores() {
        return configurationStores;
    }
    public String getStartServiceScript() {
        return startServiceScript;
    }

    @Override
    public String toString() {
        return "ServiceConfig{" +
                "name='" + name + '\'' +
                ", changedTimestamp='" + changedTimestamp + '\'' +
                ", downloadItems=" + downloadItems +
                ", startServiceScript='" + startServiceScript + '\'' +
                '}';
    }


}
