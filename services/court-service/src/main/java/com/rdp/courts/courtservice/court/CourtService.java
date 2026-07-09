package com.rdp.courts.courtservice.court;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
class CourtService {
    private final CourtRepository courtRepository;

    CourtService(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    List<Court> getActiveCourts() {
        return courtRepository.findByIsActiveTrue();
    }

    Optional<Court> getCourtById(UUID id) {
        return courtRepository.findById(id);
    }
}
