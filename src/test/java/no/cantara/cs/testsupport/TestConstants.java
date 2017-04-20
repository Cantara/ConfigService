package no.cantara.cs.testsupport;

import no.cantara.cs.admin.ApplicationResource;
import no.cantara.cs.client.ClientResource;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2017-04-20
 */
public final class TestConstants {
    public static final String[] ADMIN_PATHS = new String[]{
            ApplicationResource.APPLICATION_PATH,
            ApplicationResource.APPLICATION_PATH + "/config",
            ApplicationResource.APPLICATION_PATH + "/app1/status",
            ApplicationResource.APPLICATION_PATH + "/app1/config",
            ApplicationResource.APPLICATION_PATH + "/app1/config/appconfig1",

            ClientResource.CLIENT_PATH,
            ClientResource.CLIENT_PATH + "/1",
            ClientResource.CLIENT_PATH + "/1/env",
            ClientResource.CLIENT_PATH + "/1/status",
            ClientResource.CLIENT_PATH + "/1/config",
            ClientResource.CLIENT_PATH + "/1/events"
    };
}
