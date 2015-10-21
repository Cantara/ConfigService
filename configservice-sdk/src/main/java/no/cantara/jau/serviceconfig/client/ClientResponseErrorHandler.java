package no.cantara.jau.serviceconfig.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.NoContentException;
import java.net.HttpURLConnection;
import java.rmi.UnexpectedException;

public class ClientResponseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ClientResponseErrorHandler.class);

    public static void handle(int responseCode, String responseMessage, String url) throws UnexpectedException, NoContentException {
        log.warn("registerClient failed. url={}, responseCode={}, responseMessage={}",
                url, responseCode, responseMessage);

        if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new BadRequestException(responseMessage);
        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NotFoundException(responseMessage);
        } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            throw new InternalServerErrorException(responseMessage);
        } else if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
            throw new NoContentException(responseMessage);
        } else if (responseCode == HttpURLConnection.HTTP_PRECON_FAILED) {
            throw new IllegalStateException("412 http precondition failed. Client not registered in ConfigServer.");
        } else {
            log.warn("checkForUpdate failed. responseCode={}, responseMessage={}", responseCode, responseMessage);
            throw new UnexpectedException("Got unexpected responseCode: " + responseCode + ", with message: " +
                    responseMessage);
        }
    }
}
