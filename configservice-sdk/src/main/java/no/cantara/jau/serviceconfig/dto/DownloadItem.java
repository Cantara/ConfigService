package no.cantara.jau.serviceconfig.dto;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class DownloadItem {
    private String url;
    private String username;
    private String password;
    private String filename;

    //for jackson
    private DownloadItem() {
    }

    public DownloadItem(String url, String username, String password, String filename) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.filename = filename;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUrl() {
        return url;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getFilename() {
        return filename;
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
