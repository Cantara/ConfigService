package no.cantara.jau;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09
 */
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("overriddenByWebXml")
public class JerseyApplication extends ResourceConfig {
    private static final Logger log = LoggerFactory.getLogger(JerseyApplication.class);

    public JerseyApplication() {
        //https://java.net/jira/browse/JERSEY-2175
        //Looks like recursive scanning is not working when specifying multiple packages.
        ResourceConfig resourceConfig = packages("no.cantara.jau");
        log.debug(this.getClass().getSimpleName() + " started!");
    }
}
