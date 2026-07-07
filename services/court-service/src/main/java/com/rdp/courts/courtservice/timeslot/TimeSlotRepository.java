package com.rdp.courts.courtservice.timeslot;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdp.courts.courtservice.court.Court;

interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {
    List<TimeSlot> findByCourt(Court court);
}
