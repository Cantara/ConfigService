package no.cantara.jau.serviceconfig.client;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.NoContentException;
import java.net.HttpURLConnection;
import java.rmi.UnexpectedException;

public class ClientResponseErrorHandlerTest {

    @Test
    public void testBadRequest() {
        try {
            ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_BAD_REQUEST, "", "");
            fail("Method didn't throw as expected");
        } catch (BadRequestException e) {
            // Everything went as expected.
        } catch (Exception e) {
            fail("Method threw another method than expected");
        }
    }

    @Test
    public void testNotFound() {
        try {
            ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_NOT_FOUND, "", "");
            fail("Method didn't throw as expected");
        } catch (NotFoundException e) {
            // Everything went as expected.
        } catch (Exception e) {
            fail("Method threw another method than expected");
        }
    }

    @Test
    public void testInternalError() {
        try {
            ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_INTERNAL_ERROR, "", "");
            fail("Method didn't throw as expected");
        } catch (InternalServerErrorException e) {
            // Everything went as expected.
        } catch (Exception e) {
            fail("Method threw another method than expected");
        }
    }

    @Test
    public void testNoContent() {
        try {
            ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_NO_CONTENT, "", "");
            fail("Method didn't throw as expected");
        } catch (NoContentException e) {
            // Everything went as expected.
        } catch (Exception e) {
            fail("Method threw another method than expected");
        }
    }

    @Test
    public void testPreconditionFailed() {
        try {
            ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_PRECON_FAILED, "", "");
            fail("Method didn't throw as expected");
        } catch (IllegalStateException e) {
            // Everything went as expected.
        } catch (Exception e) {
            fail("Method threw another method than expected");
        }
    }

    @Test
    public void testUnhandledCode() {
        try {
            ClientResponseErrorHandler.handle(HttpURLConnection.HTTP_BAD_GATEWAY, "", "");
            fail("Method didn't throw as expected");
        } catch (UnexpectedException e) {
            // Everything went as expected.
        } catch (Exception e) {
            fail("Method threw another method than expected");
        }
    }
}
