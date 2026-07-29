package com.rdp.courts.courtservice.timeslot;

import java.time.LocalTime;
import java.util.UUID;

record TimeSlotResponse(UUID id, UUID courtId, LocalTime slotStart, LocalTime slotEnd) {
}
