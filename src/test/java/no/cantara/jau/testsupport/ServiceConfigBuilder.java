package no.cantara.jau.testsupport;

import no.cantara.jau.serviceconfig.dto.*;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionTag;

import java.util.HashMap;

public class ServiceConfigBuilder {



    public static ServiceConfig createServiceConfigDto(String identifier, Application application) {
        MavenMetadata metadata = new MavenMetadata("net.whydah.identity", application.artifactId, "2.0.1.Final");
        String url = new NexusUrlBuilder("http://mvnrepo.cantara.no", "releases").build(metadata);
        DownloadItem downloadItem = new DownloadItem(url, null, null, metadata);
        EventExtractionConfig extractionConfig = new EventExtractionConfig("jau");
        EventExtractionTag tag = new EventExtractionTag("testtag", "\\bheihei\\b", "logs/blabla.logg");
        extractionConfig.addEventExtractionTag(tag);

        ServiceConfig serviceConfig = new ServiceConfig(metadata.artifactId + "_" + metadata.version + "-" + identifier);
        serviceConfig.addDownloadItem(downloadItem);
        serviceConfig.addEventExtractionConfig(extractionConfig);
        serviceConfig.setStartServiceScript("java -DIAM_MODE=DEV -jar " + downloadItem.filename());

        HashMap<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put("a", "1");
        propertiesMap.put("b", "2");
        serviceConfig.addPropertiesStore(new NamedPropertiesStore(propertiesMap, "named_properties_store.properties"));

        return serviceConfig;
    }
}
