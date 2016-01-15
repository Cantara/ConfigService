package no.cantara.jau.serviceconfig.dto;

import no.cantara.jau.serviceconfig.dto.event.EventExtractionConfig;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class ServiceConfig implements Serializable {

	private static final long serialVersionUID = -7717945124215182099L;

	private static final DateTimeFormatter df = DateTimeFormatter.ISO_INSTANT;

    private String id;

    /**
     * Human readable name of this service config.
     * For example "artifactId-version" of executable jar file.
     */
    private String name;
    /**
     * A timestamp when the config was last changed.
     * ISO 8601 combined format, see https://en.wikipedia.org/?title=ISO_8601#Combined_date_and_time_representations
     * For example "2007-04-05T14:30".
     */
    private String lastChanged;
    private List<DownloadItem> downloadItems;
    private List<NamedPropertiesStore> configurationStores;
    private List<EventExtractionConfig> eventExtractionConfigs;

    private String startServiceScript;

    //for jackson
    private ServiceConfig() {
    }

    public ServiceConfig(String name) {
        this.name = name;
        setUpdated();
        this.downloadItems = new ArrayList<>();
        this.configurationStores = new ArrayList<>();
        this.eventExtractionConfigs = new ArrayList<>();
    }

    private void setUpdated() {
        this.lastChanged = df.format(Instant.now());
    }

    public void addDownloadItem(DownloadItem downloadItem) {
        downloadItems.add(downloadItem);
    }

    public void addPropertiesStore(NamedPropertiesStore propertiesStore) {
        configurationStores.add(propertiesStore);
    }

    public void addEventExtractionConfig(EventExtractionConfig eventExtractionConfig) {
        eventExtractionConfigs.add(eventExtractionConfig);
    }

    public void setId(String id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    //jackson
    private void setLastChanged(String lastChanged) {
        this.lastChanged = lastChanged;
    }
    //jackson
    private void setDownloadItems(List<DownloadItem> downloadItems) {
        this.downloadItems = downloadItems;

    }
    public void setStartServiceScript(String startServiceScript) {
        this.startServiceScript = startServiceScript;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getLastChanged() {
        return lastChanged;
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
    public List<EventExtractionConfig> getEventExtractionConfigs() {
        return eventExtractionConfigs;
    }

    @Override
    public String toString() {
        return "ServiceConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", lastChanged='" + lastChanged + '\'' +
                ", downloadItemCount=" + downloadItems.size() +
                ", configurationStores=" + configurationStores +
                ", eventExtractionConfigs=" + eventExtractionConfigs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceConfig that = (ServiceConfig) o;

        if (!id.equals(that.id)) return false;
        if (!name.equals(that.name)) return false;
        if (downloadItems != null ? !downloadItems.equals(that.downloadItems) : that.downloadItems != null) return false;
        if (configurationStores != null ? !configurationStores.equals(that.configurationStores) : that.configurationStores != null)
            return false;
        if (eventExtractionConfigs != null ? !eventExtractionConfigs.equals(that.eventExtractionConfigs) : that.eventExtractionConfigs != null)
            return false;
        return startServiceScript.equals(that.startServiceScript);

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (downloadItems != null ? downloadItems.hashCode() : 0);
        result = 31 * result + (configurationStores != null ? configurationStores.hashCode() : 0);
        result = 31 * result + (eventExtractionConfigs != null ? eventExtractionConfigs.hashCode() : 0);
        result = 31 * result + startServiceScript.hashCode();
        return result;
    }
}
