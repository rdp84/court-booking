package com.rdp.courts.courtservice.pricing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CourtPricingRepository extends JpaRepository<CourtPricing, UUID> {
    @Query("SELECT cp FROM CourtPricing cp WHERE cp.dayType = :dayType " +
            "AND cp.periodStart <= :time AND cp.periodEnd > :time " +
            "AND cp.validFrom <= :date " +
            "ORDER BY cp.validFrom DESC LIMIT 1")
    Optional<CourtPricing> findApplicablePricing(
            @Param("dayType") DayType dayType,
            @Param("time") LocalTime time,
            @Param("date") LocalDate date);
}
