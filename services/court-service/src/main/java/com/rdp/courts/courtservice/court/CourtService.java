package com.rdp.courts.courtservice.court;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class CourtService {
    private final CourtRepository courtRepository;

    CourtService(final CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    public Optional<Court> getCourtById(final UUID id) {
        return courtRepository.findById(id);
    }

    List<Court> getActiveCourts() {
        return courtRepository.findByIsActiveTrue();
    }
}
