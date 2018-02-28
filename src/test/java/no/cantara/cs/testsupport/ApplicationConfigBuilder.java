package no.cantara.cs.testsupport;

import no.cantara.cs.dto.Application;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.DownloadItem;
import no.cantara.cs.dto.MavenMetadata;
import no.cantara.cs.dto.NamedPropertiesStore;
import no.cantara.cs.dto.NexusUrlBuilder;
import no.cantara.cs.dto.event.EventExtractionConfig;
import no.cantara.cs.dto.event.EventExtractionTag;

import java.util.LinkedHashMap;

public class ApplicationConfigBuilder {

    public static ApplicationConfig createConfigDto(String name, Application application) {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", application.artifactId, "2.0.1.Final");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "releases").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);
        EventExtractionConfig extractionConfig = new EventExtractionConfig("jau");
        EventExtractionTag tag = new EventExtractionTag("testtag", "\\bheihei\\b", "logs/blabla.logg");
        extractionConfig.addEventExtractionTag(tag);

        ApplicationConfig config = new ApplicationConfig(name);
        config.addDownloadItem(downloadItem);
        config.addEventExtractionConfig(extractionConfig);
        config.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());

        LinkedHashMap<String, String> propertiesMap = new LinkedHashMap<>();
        propertiesMap.put("a", "1");
        propertiesMap.put("b", "2");
        config.addPropertiesStore(new NamedPropertiesStore(propertiesMap, "named_properties_store.properties"));

        return config;
    }
}
