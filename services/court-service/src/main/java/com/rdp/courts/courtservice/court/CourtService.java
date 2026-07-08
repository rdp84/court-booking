package com.rdp.courts.courtservice.court;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
class CourtService {
    private final CourtRepository courtRepository;

    CourtService(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    List<CourtResponse> getActiveCourts() {
        return courtRepository.findByIsActiveTrue().stream()
                .map(court -> new CourtResponse(court.getId(), court.getName(), court.getIsActive())).toList();
    }
}
