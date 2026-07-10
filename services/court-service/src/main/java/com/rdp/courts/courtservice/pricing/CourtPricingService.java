package com.rdp.courts.courtservice.pricing;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public class CourtPricingService {
    private final CourtPricingRepository courtPricingRepository;

    CourtPricingService(CourtPricingRepository courtPricingRepository) {
        this.courtPricingRepository = courtPricingRepository;
    }

    public Optional<BigDecimal> calculateFee(final LocalDate day, final LocalTime time) {
        final var dayType = isWeekend(day.getDayOfWeek()) ? DayType.WEEKEND : DayType.WEEKDAY;

        return courtPricingRepository.findApplicablePricing(dayType, time, day)
                .map(courtPricing -> courtPricing.getFee());
    }

    private boolean isWeekend(final DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }
}
