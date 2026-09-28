package com.rdp.bookings.bookingservice.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

interface CourtServiceClient {
    Optional<CourtResponse> getCourt(UUID courtId);

    Optional<TimeSlotResponse> getTimeSlot(UUID timeSlotId);

    Optional<CourtPricingResponse> getPricing(LocalDate date, LocalTime time);
}
