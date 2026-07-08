package com.rdp.courts.courtservice.court;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByIsActiveTrue();
}
