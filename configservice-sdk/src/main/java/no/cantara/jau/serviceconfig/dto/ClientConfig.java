package no.cantara.jau.serviceconfig.dto;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-08-23.
 */
public class ClientConfig {
    public String clientId;
    public ServiceConfig serviceConfig;

    //for jackson
    private ClientConfig() {
    }

    public ClientConfig(String clientId, ServiceConfig serviceConfig) {
        this.clientId = clientId;
        this.serviceConfig = serviceConfig;
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "clientId='" + clientId + '\'' +
                ", " + serviceConfig +
                '}';
    }
}
