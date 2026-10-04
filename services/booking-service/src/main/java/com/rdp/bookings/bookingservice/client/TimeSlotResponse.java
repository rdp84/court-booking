package com.rdp.bookings.bookingservice.client;

import java.time.LocalTime;
import java.util.UUID;

public record TimeSlotResponse(UUID id, UUID courtId, LocalTime slotStart, LocalTime slotEnd) {
}
