package com.rdp.courts.courtservice.court;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{id}")
    ResponseEntity<CourtResponse> getCourt(@PathVariable final UUID id) {
        return courtService.getCourtById(id).map(this::toCourtResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    List<CourtResponse> getActiveCourts() {
        return courtService.getActiveCourts().stream().map(this::toCourtResponse).toList();
    }

    private CourtResponse toCourtResponse(final Court court) {
        return new CourtResponse(court.getId(), court.getName(), court.isActive());
    }
}
