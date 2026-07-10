package com.rdp.courts.courtservice.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtPricingServiceTest {

    @Mock
    CourtPricingRepository courtPricingRepository;

    @InjectMocks
    CourtPricingService courtPricingService;

    @Test
    void shouldReturnFeeForWeekdayOffPeak() {
        final var id = UUID.randomUUID();
        final var courtPricing = new CourtPricing(id, DayType.WEEKDAY, LocalTime.of(6, 45), LocalTime.of(17, 0),
                new BigDecimal("3.00"), LocalDate.of(2000, 1, 1));

        given(courtPricingRepository.findApplicablePricing(DayType.WEEKDAY, LocalTime.of(12, 0),
                LocalDate.of(2000, 1, 3))).willReturn(Optional.of(courtPricing));

        final var result = courtPricingService.calculateFee(LocalDate.of(2000, 1, 3), LocalTime.of(12, 0));
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void shouldReturnFeeForWeekdayPeak() {
        final var id = UUID.randomUUID();
        final var courtPricing = new CourtPricing(id, DayType.WEEKDAY, LocalTime.of(17, 0), LocalTime.of(21, 0),
                new BigDecimal("6.00"), LocalDate.of(2000, 1, 1));

        given(courtPricingRepository.findApplicablePricing(DayType.WEEKDAY, LocalTime.of(17, 15),
                LocalDate.of(2000, 1, 4))).willReturn(Optional.of(courtPricing));

        final var result = courtPricingService.calculateFee(LocalDate.of(2000, 1, 4), LocalTime.of(17, 15));
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    void shouldReturnFeeForWeekend() {
        final var id = UUID.randomUUID();
        final var courtPricing = new CourtPricing(id, DayType.WEEKEND, LocalTime.of(6, 45), LocalTime.of(22, 45),
                new BigDecimal("3.00"), LocalDate.of(2000, 1, 1));

        given(courtPricingRepository.findApplicablePricing(DayType.WEEKEND, LocalTime.of(7, 30),
                LocalDate.of(2000, 1, 2))).willReturn(Optional.of(courtPricing));

        final var result = courtPricingService.calculateFee(LocalDate.of(2000, 1, 2), LocalTime.of(7, 30));
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    void shouldReturnEmptyWhenNoCourtPricingFound() {
        given(courtPricingRepository.findApplicablePricing(DayType.WEEKEND, LocalTime.of(3, 30),
                LocalDate.of(2000, 1, 2))).willReturn(Optional.empty());

        final var result = courtPricingService.calculateFee(LocalDate.of(2000, 1, 2), LocalTime.of(3, 30));
        assertThat(result).isNotPresent();
    }
}
