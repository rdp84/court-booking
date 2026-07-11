package com.rdp.courts.courtservice.timeslot;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.courts.courtservice.court.CourtService;

@RestController
@RequestMapping("/courts")
class TimeSlotController {
    private final TimeSlotService timeSlotService;
    private final CourtService courtService;

    TimeSlotController(TimeSlotService timeSlotService, CourtService courtService) {
        this.timeSlotService = timeSlotService;
        this.courtService = courtService;
    }

    @GetMapping("/{id}/slots")
    ResponseEntity<List<TimeSlotResponse>> getTimeSlotsForCourt(@PathVariable UUID id) {
        return courtService.getCourtById(id)
                .map(court -> timeSlotService.getTimeSlotsForCourt(court))
                .map(slots -> slots.stream()
                        .map(slot -> new TimeSlotResponse(slot.getId(), slot.getSlotStart(), slot.getSlotEnd()))
                        .toList())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
