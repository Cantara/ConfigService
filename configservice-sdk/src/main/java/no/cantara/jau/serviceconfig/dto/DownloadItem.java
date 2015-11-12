package no.cantara.jau.serviceconfig.dto;

import java.io.Serializable;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class DownloadItem implements Serializable {
	
	private static final long serialVersionUID = -6920085900315128790L;
	
	public String url;
    public String username;
    public String password;
    private String filename;

    public MavenMetadata metadata;

    //for jackson
    private DownloadItem() {
    }

    public DownloadItem(String url, String username, String password, String filename) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.filename = filename;
    }
    public DownloadItem(String url, String username, String password, MavenMetadata metadata) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.metadata = metadata;
    }

    public String filename() {
        if (filename != null) {
            return filename;
        }
        if (metadata != null) {
            return metadata.filename();
        }
        return null;
    }

    @Override
    public String toString() {
        return "DownloadItem{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", filename='" + filename + '\'' +
                '}';
    }
}
