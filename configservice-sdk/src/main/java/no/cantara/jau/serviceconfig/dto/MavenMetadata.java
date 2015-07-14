package no.cantara.jau.serviceconfig.dto;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-14.
 */
public class MavenMetadata {
    public String groupId;
    public String artifactId;
    public String version;
    public String packaging = "jar";
    public String lastUpdated;
    public String buildNumber;

    //for jackson
    private MavenMetadata() {
    }

    public MavenMetadata(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }
    public MavenMetadata(String groupId, String artifactId, String version, String packaging) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
    }

    public String filename() {
        return  artifactId + "-" + version + "." + packaging;
    }
    public String timestampedFilename() {
        return  artifactId + "-" + lastUpdated + "-" + buildNumber + "." + packaging;
    }
}
