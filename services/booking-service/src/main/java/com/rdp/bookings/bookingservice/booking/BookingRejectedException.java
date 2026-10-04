package com.rdp.bookings.bookingservice.booking;

class BookingRejectedException extends RuntimeException {
    private final RejectionReason reason;

    BookingRejectedException(final RejectionReason reason, final String message) {
        super(message);
        this.reason = reason;
    }

    BookingRejectedException(final RejectionReason reason, final String message, final Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    RejectionReason getReason() {
        return reason;
    }
}
