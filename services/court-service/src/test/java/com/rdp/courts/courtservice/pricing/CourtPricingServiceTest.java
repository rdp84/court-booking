package com.rdp.courts.courtservice.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CourtPricingServiceTest {
    @Nested
    @DisplayName("Weekday pricing")
    class WeekdayPricing {

        private static Stream<LocalTime> offPeakSlots() {
            return Stream.of(
                    LocalTime.of(6, 45),
                    LocalTime.of(16, 30),
                    LocalTime.of(21, 0),
                    LocalTime.of(22, 15));
        }

        private static Stream<LocalTime> peakSlots() {
            return Stream.of(
                    LocalTime.of(17, 0),
                    LocalTime.of(17, 15),
                    LocalTime.of(20, 15),
                    LocalTime.of(20, 45));
        }

        @ParameterizedTest
        @DisplayName("Off-peak slots should return £3.00")
        @MethodSource("offPeakSlots")
        void shouldReturnOffPeakFee(final LocalTime slot) {
            final var fee = pricingService.calculateFee(WEEKDAY, slot);
            assertThat(fee).isEqualByComparingTo(CourtPricingService.OFF_PEAK_FEE);
        }

        @ParameterizedTest
        @DisplayName("Peak slots should return £6.00")
        @MethodSource("peakSlots")
        void shouldReturnPeakFee(final LocalTime slot) {
            final var fee = pricingService.calculateFee(WEEKDAY, slot);
            assertThat(fee).isEqualByComparingTo(CourtPricingService.PEAK_FEE);
        }
    }

    @Nested
    @DisplayName("Weekend pricing")
    class WeekendPricing {

        private static Stream<LocalTime> weekendSlots() {
            return Stream.of(
                    LocalTime.of(7, 15),
                    LocalTime.of(12, 0),
                    LocalTime.of(16, 30),
                    LocalTime.of(17, 0),
                    LocalTime.of(17, 15),
                    LocalTime.of(20, 15),
                    LocalTime.of(20, 45),
                    LocalTime.of(21, 0),
                    LocalTime.of(22, 15));
        }

        @ParameterizedTest
        @DisplayName("All Saturday slots should return £3.00")
        @MethodSource("weekendSlots")
        void shouldReturnOffPeakFeeForSaturday(final LocalTime slot) {
            final var saturday = LocalDate.of(2000, 1, 8);
            final var fee = pricingService.calculateFee(saturday, slot);
            assertThat(fee).isEqualByComparingTo(CourtPricingService.OFF_PEAK_FEE);
        }

        @ParameterizedTest
        @DisplayName("All Sunday slots should return £3.00")
        @MethodSource("weekendSlots")
        void shouldReturnOffPeakFeeForSunday(final LocalTime slot) {
            final var sunday = LocalDate.of(2000, 1, 9);
            final var fee = pricingService.calculateFee(sunday, slot);
            assertThat(fee).isEqualByComparingTo(CourtPricingService.OFF_PEAK_FEE);
        }
    }

    @Nested
    @DisplayName("Null input")
    class NullInput {

        @Test
        void shouldThrowExceptionWhenDayIsNull() {
            assertThatThrownBy(() -> pricingService.calculateFee(null, LocalTime.of(8, 15)))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("day must not be null");
        }

        @Test
        void shouldThrowExceptionWhenTimeIsNull() {
            assertThatThrownBy(() -> pricingService.calculateFee(LocalDate.of(2000, 1, 4), null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("time must not be null");
        }
    }

    private static final LocalDate WEEKDAY = LocalDate.of(2000, 1, 3); // Monday

    private final CourtPricingService pricingService = new CourtPricingService();
}
