package com.rdp.courts.courtservice;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CourtPricingServiceTest {

    private final CourtPricingService pricingService = new CourtPricingService();

    @Test
    void shouldReturnOffPeakFeeForWeekdayMorningSlot() {
        LocalDate weekday = LocalDate.of(2026, 7, 6); // Monday
        LocalTime morningSlot = LocalTime.of(6, 45);

        BigDecimal fee = pricingService.calculateFee(weekday, morningSlot);

        assertThat(fee).isEqualByComparingTo(new BigDecimal("3.00"));
    }
}
