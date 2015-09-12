package no.cantara.jau.serviceconfig.dto;

import java.text.MessageFormat;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class NexusUrlBuilder {
    private final String baseUrl;   // http://repository.example.com:8081/nexus
    private static final String REST_PATH="/service/local";
    private static final String ART_REDIR="/artifact/maven/redirect";
    private final String repo;

    public NexusUrlBuilder(String baseUrl, String repo) {
        this.baseUrl = baseUrl;
        this.repo = repo;
    }

    /*
     * http://blog.sonatype.com/2011/01/downloading-artifacts-from-nexus-with-bash/#.VZ5Oq7waa90
     * Output example: http://mvnrepo.cantara.no/service/local/artifact/maven/redirect?r=snapshots&g=net.whydah.identity&a=UserAdminService&v=2.1-SNAPSHOT&p=jar
     */
    public String build(String groupId, String artifactId, String version, String packaging) {
        String theRest = MessageFormat.format("?r={0}&g={1}&a={2}&v={3}&p={4}", repo, groupId, artifactId, version, packaging);
        return baseUrl + REST_PATH + ART_REDIR + theRest;
    }

    public String build(MavenMetadata metadata) {
        String theRest = MessageFormat.format("?r={0}&g={1}&a={2}&v={3}&p={4}", repo, metadata.groupId, metadata.artifactId, metadata.version, metadata.packaging);
        return baseUrl + REST_PATH + ART_REDIR + theRest;
    }
}
