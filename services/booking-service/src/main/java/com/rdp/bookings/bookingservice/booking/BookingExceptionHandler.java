package com.rdp.bookings.bookingservice.booking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class BookingExceptionHandler {

    // The reason is included in the body because some statuses (e.g. 409) cover more than one rejection
    @ExceptionHandler(BookingRejectedException.class)
    ProblemDetail handleBookingRejected(final BookingRejectedException e) {
        final var problem = ProblemDetail.forStatusAndDetail(toStatus(e.getReason()), e.getMessage());
        problem.setProperty("reason", e.getReason());
        return problem;
    }

    private static HttpStatus toStatus(final RejectionReason reason) {
        return switch (reason) {
            case BOOKING_DATE_IN_PAST, OPPONENT_IS_BOOKER -> HttpStatus.BAD_REQUEST;
            case NOT_BOOKER -> HttpStatus.FORBIDDEN;
            case COURT_NOT_FOUND, TIME_SLOT_NOT_FOUND, MEMBER_NOT_FOUND, BOOKING_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case COURT_INACTIVE, TIME_SLOT_NOT_ON_COURT, NO_PRICING, MEMBERSHIP_INACTIVE, BOOKING_ALREADY_STARTED ->
                HttpStatus.UNPROCESSABLE_CONTENT;
            case SLOT_ALREADY_BOOKED, MEMBER_HAS_OVERLAPPING_BOOKING, ALREADY_CANCELLED -> HttpStatus.CONFLICT;
        };
    }
}
