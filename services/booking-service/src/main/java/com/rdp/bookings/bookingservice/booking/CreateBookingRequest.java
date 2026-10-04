package com.rdp.bookings.bookingservice.booking;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

// opponentMemberId is optional: null represents a guest opponent
record CreateBookingRequest(@NotNull UUID courtId, @NotNull UUID timeSlotId, @NotNull LocalDate bookingDate,
        @NotNull UUID bookerMemberId, UUID opponentMemberId) {
}
