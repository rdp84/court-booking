package com.rdp.bookings.bookingservice.client;

public class UpstreamServiceException extends RuntimeException {
    UpstreamServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
