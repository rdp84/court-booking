package com.rdp.courts.courtservice.timeslot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.rdp.courts.courtservice.court.Court;

@Service
class TimeSlotService {
    private final TimeSlotRepository timeSlotRepository;

    TimeSlotService(final TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    Optional<TimeSlot> getTimeSlotById(final UUID id) {
        return timeSlotRepository.findById(id).filter(slot -> slot.getCourt().isActive());
    }

    List<TimeSlot> getTimeSlotsForCourt(final Court court) {
        return timeSlotRepository.findByCourt(court);
    }
}
