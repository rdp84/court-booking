package com.rdp.courts.courtservice.timeslot;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/timeslots")
class TimeSlotLookupController {
    private final TimeSlotService timeSlotService;

    TimeSlotLookupController(final TimeSlotService timeSlotService) {
        this.timeSlotService = timeSlotService;
    }

    @GetMapping("/{id}")
    ResponseEntity<TimeSlotResponse> getTimeSlot(@PathVariable final UUID id) {
        return timeSlotService
                .getTimeSlotById(id).map(slot -> new TimeSlotResponse(slot.getId(), slot.getCourt().getId(),
                        slot.getSlotStart(), slot.getSlotEnd()))
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
