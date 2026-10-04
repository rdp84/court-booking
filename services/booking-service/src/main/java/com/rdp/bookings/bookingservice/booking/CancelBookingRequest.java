package com.rdp.bookings.bookingservice.booking;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

// memberId must be the booker; it will come from authentication once that exists
record CancelBookingRequest(@NotNull UUID memberId) {
}
