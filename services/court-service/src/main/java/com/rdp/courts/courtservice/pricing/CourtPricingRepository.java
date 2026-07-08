package com.rdp.courts.courtservice.pricing;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface CourtPricingRepository extends JpaRepository<CourtPricing, UUID> {
}
