package no.cantara.jau.serviceconfig.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.net.HttpURLConnection;
import java.rmi.UnexpectedException;

public class ResponseErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseErrorHandler.class);


    public static void handle(int responseCode, String responseMessage, String url) throws UnexpectedException {
        log.warn("registerClient failed. url={}, responseCode={}, responseMessage={}",
                url, responseCode, responseMessage);

        if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new BadRequestException(responseMessage);
        } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NotFoundException(responseMessage);
        } else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            throw new InternalServerErrorException(responseMessage);
        } else {
            throw new UnexpectedException("Got code: " + responseCode + ", and message: " +
                    responseMessage);
        }
    }
}
