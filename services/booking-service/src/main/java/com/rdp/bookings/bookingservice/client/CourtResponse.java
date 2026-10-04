package com.rdp.bookings.bookingservice.client;

import java.util.UUID;

public record CourtResponse(UUID id, String name, boolean isActive) {
}
