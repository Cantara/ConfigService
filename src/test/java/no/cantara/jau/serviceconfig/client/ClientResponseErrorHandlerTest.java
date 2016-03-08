package no.cantara.jau.serviceconfig.client;

import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.NoContentException;
import java.net.HttpURLConnection;
import java.rmi.UnexpectedException;

public class ClientResponseErrorHandlerTest {
    @Test(expectedExceptions = BadRequestException.class)
    public void testBadRequest() throws UnexpectedException, NoContentException {
        ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_BAD_REQUEST, "", "", "");
    }

    @Test(expectedExceptions = NotFoundException.class)
    public void testNotFound() throws UnexpectedException, NoContentException {
        ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_NOT_FOUND, "", "", "");
    }

    @Test(expectedExceptions = InternalServerErrorException.class)
    public void testInternalError() throws UnexpectedException, NoContentException {
        ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_INTERNAL_ERROR, "", "", "");
    }

    @Test(expectedExceptions = NoContentException.class)
    public void testNoContent() throws UnexpectedException, NoContentException {
        ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_NO_CONTENT, "", "", "");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testPreconditionFailed() throws UnexpectedException, NoContentException {
        ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_PRECON_FAILED, "", "", "");
    }

    @Test(expectedExceptions = UnexpectedException.class)
    public void testUnhandledCode() throws UnexpectedException, NoContentException {
        ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_BAD_GATEWAY, "", "", "");
    }
}
