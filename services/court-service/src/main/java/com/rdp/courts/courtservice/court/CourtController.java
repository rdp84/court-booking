package com.rdp.courts.courtservice.court;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courts")
class CourtController {
    private record CourtResponse(UUID id, String name, boolean isActive) {
    }

    private final CourtService courtService;

    CourtController(final CourtService courtService) {
        this.courtService = courtService;
    }

    @GetMapping
    List<CourtResponse> getActiveCourts() {
        return courtService.getActiveCourts().stream()
                .map(court -> new CourtResponse(court.getId(), court.getName(), court.isActive())).toList();
    }
}
