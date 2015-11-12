package no.cantara.jau.serviceconfig.dto;

import java.io.Serializable;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-12.
 */
public class Application implements Serializable {
	
	private static final long serialVersionUID = -5653166759451238064L;
	
	public String id;
    public String artifactId;

    //for jackson
    private Application() {
    }

    public Application(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public String toString() {
        return "Application{" +
                "id='" + id + '\'' +
                ", artifactId='" + artifactId + '\'' +
                '}';
    }
}
