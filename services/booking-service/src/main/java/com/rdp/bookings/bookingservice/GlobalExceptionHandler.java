package com.rdp.bookings.bookingservice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.rdp.bookings.bookingservice.client.UpstreamServiceException;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(HttpServerErrorException.class)
    ResponseEntity<Void> handleUpstreamServerError(final HttpServerErrorException e) {
        return ResponseEntity.status(502).build();
    }

    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Void> handleUpstreamUnavailable(final ResourceAccessException e) {
        return ResponseEntity.status(503).build();
    }

    @ExceptionHandler(UpstreamServiceException.class)
    ResponseEntity<Void> handleUpstreamError(final UpstreamServiceException e) {
        return ResponseEntity.status(502).build();
    }
}
