package com.rdp.courts.courtservice.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;

public class CourtPricingService {
    static final BigDecimal OFF_PEAK_FEE = new BigDecimal("3.00");
    static final BigDecimal PEAK_FEE = new BigDecimal("6.00");

    public BigDecimal calculateFee(LocalDate day, LocalTime time) {
        if (day == null)
            throw new IllegalArgumentException("day must not be null");
        if (time == null)
            throw new IllegalArgumentException("time must not be null");

        var dayOfWeek = day.getDayOfWeek();
        var hour = time.getHour();

        if (isWeekend(dayOfWeek) || isOffPeakHour(hour)) {
            return OFF_PEAK_FEE;
        }
        return PEAK_FEE;
    }

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY;
    }

    private boolean isOffPeakHour(int hour) {
        return hour < 17 || hour >= 21;
    }
}
