package com.rdp.bookings.bookingservice.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

record BookingResponse(UUID id, UUID courtId, UUID timeSlotId, LocalTime slotStart, LocalTime slotEnd,
        LocalDate bookingDate, UUID bookerMemberId, UUID opponentMemberId, BookingStatus status, BigDecimal courtFee,
        LocalDateTime createdAt, LocalDateTime cancelledAt) {
}
