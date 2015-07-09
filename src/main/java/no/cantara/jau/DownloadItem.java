package no.cantara.jau;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-09.
 */
public class DownloadItem {
    private String url;
    private String username;
    private String password;

    //for jackson
    private DownloadItem() {
    }

    public DownloadItem(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
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

    public String getUrl() {
        return url;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
}
