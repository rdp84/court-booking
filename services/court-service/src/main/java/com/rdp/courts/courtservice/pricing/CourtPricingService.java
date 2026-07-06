package com.rdp.courts.courtservice.pricing;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class CourtPricingService {
    static final BigDecimal OFF_PEAK_FEE = new BigDecimal("3.00");
    static final BigDecimal PEAK_FEE = new BigDecimal("6.00");

    public BigDecimal calculateFee(final LocalDate day, final LocalTime time) {
        if (day == null)
            throw new IllegalArgumentException("day must not be null");
        if (time == null)
            throw new IllegalArgumentException("time must not be null");

        final var dayOfWeek = day.getDayOfWeek();
        final var hour = time.getHour();

        if (isWeekend(dayOfWeek) || isOffPeakHour(hour)) {
            return OFF_PEAK_FEE;
        }
        return PEAK_FEE;
    }

    private boolean isWeekend(final DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isOffPeakHour(final int hour) {
        return hour < 17 || hour >= 21;
    }
}
