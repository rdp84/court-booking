package com.rdp.bookings.bookingservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.rdp.bookings.bookingservice.client.UpstreamServiceException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnBadGatewayForUpstreamServerError() {
        final var exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        final var response = handler.handleUpstreamServerError(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void shouldReturnServiceUnavailableForUpstreamUnavailable() {
        final var exception = new ResourceAccessException("Connection refused");

        final var response = handler.handleUpstreamUnavailable(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldReturnBadGatewayForUpstreamServiceException() {
        final var exception = mock(UpstreamServiceException.class);

        final var response = handler.handleUpstreamError(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
