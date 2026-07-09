package com.rdp.courts.courtservice.timeslot;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rdp.courts.courtservice.court.Court;

@Service
class TimeSlotService {
    private final TimeSlotRepository timeSlotRepository;

    TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    List<TimeSlotResponse> getTimeSlotsForCourt(Court court) {
        return timeSlotRepository.findByCourt(court).stream().map(
                timeSlot -> new TimeSlotResponse(timeSlot.getId(), timeSlot.getSlotStart(), timeSlot.getSlotEnd()))
                .toList();
    }
}
