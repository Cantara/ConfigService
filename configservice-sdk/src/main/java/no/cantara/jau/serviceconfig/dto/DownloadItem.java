package no.cantara.jau.serviceconfig.dto;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class DownloadItem {
    public String url;
    public String username;
    public String password;
    public String filename;

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
