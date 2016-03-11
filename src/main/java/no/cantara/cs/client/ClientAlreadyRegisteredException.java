package no.cantara.cs.client;

public class ClientAlreadyRegisteredException extends RuntimeException {

    public ClientAlreadyRegisteredException(String clientId) {
        super("Client is already registered, clientId: " + clientId);
    }
}
