package com.rdp.courts.courtservice.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "court_pricing")
class CourtPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private DayType dayType;

    private LocalTime periodStart;
    private LocalTime periodEnd;
    private BigDecimal fee;
    private LocalDate validFrom;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Required by JPA
    CourtPricing() {
    }

    CourtPricing(DayType dayType, LocalTime periodStart, LocalTime periodEnd, BigDecimal fee, LocalDate validFrom) {
        this.dayType = dayType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.fee = fee;
        this.validFrom = validFrom;
    }

    public UUID getId() {
        return id;
    }

    public DayType getDayType() {
        return dayType;
    }

    public LocalTime getPeriodStart() {
        return periodStart;
    }

    public LocalTime getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
