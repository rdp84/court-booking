package com.rdp.courts.courtservice.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CourtPricingRepositoryTest {

    @Nested
    @DisplayName("Weekday court pricing")
    class WeekdayCourtPricing {

        private static Stream<Arguments> offPeakCourtPricing() {
            final var weekday = DayType.WEEKDAY;
            final var expectedFee = new BigDecimal("3.00");
            return Stream.of(
                    Arguments.of(weekday, LocalTime.of(6, 45), LocalDate.of(2000, 1, 3), expectedFee),
                    Arguments.of(weekday, LocalTime.of(16, 30), LocalDate.of(2000, 1, 4), expectedFee),
                    Arguments.of(weekday, LocalTime.of(21, 0), LocalDate.of(2000, 1, 5), expectedFee),
                    Arguments.of(weekday, LocalTime.of(22, 15), LocalDate.of(2000, 1, 5), expectedFee));
        }

        private static Stream<Arguments> peakCourtPricing() {
            final var weekday = DayType.WEEKDAY;
            final var expectedFee = new BigDecimal("6.00");
            return Stream.of(
                    Arguments.of(weekday, LocalTime.of(17, 0), LocalDate.of(2000, 1, 10), expectedFee),
                    Arguments.of(weekday, LocalTime.of(17, 15), LocalDate.of(2000, 1, 11), expectedFee),
                    Arguments.of(weekday, LocalTime.of(20, 15), LocalDate.of(2000, 1, 12), expectedFee),
                    Arguments.of(weekday, LocalTime.of(20, 45), LocalDate.of(2000, 1, 13), expectedFee));
        }

        @ParameterizedTest
        @DisplayName("applicable pricing for off-peak slots should be found")
        @MethodSource("offPeakCourtPricing")
        void shouldFindApplicablePricingForWeekdayOffPeak(final DayType dayType, final LocalTime time,
                final LocalDate date, final BigDecimal expectedFee) {
            shouldFindApplicablePricing(dayType, time, date, expectedFee);
        }

        @ParameterizedTest
        @DisplayName("applicable pricing peak slots should be found")
        @MethodSource("peakCourtPricing")
        void shouldFindApplicablePricingForWeekdayPeak(final DayType dayType, final LocalTime time,
                final LocalDate date, final BigDecimal expectedFee) {
            shouldFindApplicablePricing(dayType, time, date, expectedFee);
        }
    }

    @Nested
    @DisplayName("Weekend court pricing")
    class WeekendCourtPricing {

        private static Stream<Arguments> weekendCourtPricing() {
            final var weekend = DayType.WEEKEND;
            final var expectedFee = new BigDecimal("3.00");
            return Stream.of(
                    Arguments.of(weekend, LocalTime.of(7, 15), LocalDate.of(2000, 1, 1), expectedFee),
                    Arguments.of(weekend, LocalTime.of(12, 0), LocalDate.of(2000, 1, 2), expectedFee),
                    Arguments.of(weekend, LocalTime.of(16, 30), LocalDate.of(2000, 1, 8), expectedFee),
                    Arguments.of(weekend, LocalTime.of(17, 0), LocalDate.of(2000, 1, 9), expectedFee),
                    Arguments.of(weekend, LocalTime.of(17, 15), LocalDate.of(2000, 1, 15), expectedFee),
                    Arguments.of(weekend, LocalTime.of(20, 15), LocalDate.of(2000, 1, 16), expectedFee),
                    Arguments.of(weekend, LocalTime.of(20, 45), LocalDate.of(2000, 1, 22), expectedFee),
                    Arguments.of(weekend, LocalTime.of(21, 0), LocalDate.of(2000, 1, 23), expectedFee),
                    Arguments.of(weekend, LocalTime.of(22, 15), LocalDate.of(2000, 1, 24), expectedFee));
        }

        @ParameterizedTest
        @DisplayName("applicable pricing for weekend slots should be found")
        @MethodSource("weekendCourtPricing")
        void shouldFindApplicablePricingForWeekend(final DayType dayType, final LocalTime time,
                final LocalDate date, final BigDecimal expectedFee) {
            shouldFindApplicablePricing(dayType, time, date, expectedFee);
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    CourtPricingRepository courtPricingRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveCourtPricing() {
        final var courtPricing = new CourtPricing(DayType.WEEKDAY, LocalTime.of(0, 0), LocalTime.of(1, 30),
                new BigDecimal("3.00"), LocalDate.of(2000, 1, 1));
        courtPricingRepository.save(courtPricing);
        entityManager.flush();
        entityManager.clear();

        final var retrieved = courtPricingRepository.findById(courtPricing.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isNotNull();
        assertThat(retrieved.get().getDayType()).isEqualTo(DayType.WEEKDAY);
        assertThat(retrieved.get().getPeriodStart()).isEqualTo(LocalTime.of(0, 0));
        assertThat(retrieved.get().getPeriodEnd()).isEqualTo(LocalTime.of(1, 30));
        assertThat(retrieved.get().getFee()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(retrieved.get().getValidFrom()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(retrieved.get().getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldReturnEmptyWhenNoCourtPricingFound() {
        final var retrieved = courtPricingRepository.findApplicablePricing(DayType.WEEKDAY, LocalTime.of(3, 30),
                LocalDate.of(2000, 1, 3));
        assertThat(retrieved).isNotPresent();
    }

    @Test
    void shouldThrowConstraintViolationWhenDuplicateDayStartValidFrom() {
        courtPricingRepository.save(new CourtPricing(DayType.WEEKEND, LocalTime.of(0, 0), LocalTime.of(1, 30),
                new BigDecimal("3.00"), LocalDate.of(2000, 1, 1)));
        entityManager.flush();
        entityManager.clear();

        courtPricingRepository.save(new CourtPricing(DayType.WEEKEND, LocalTime.of(0, 0), LocalTime.of(1, 30),
                new BigDecimal("3.00"), LocalDate.of(2000, 1, 1)));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName()).isEqualTo("uq_pricing_period");
    }

    @Test
    void shouldThrowConstraintViolationWhenEndBeforeStart() {
        courtPricingRepository.save(new CourtPricing(DayType.WEEKDAY, LocalTime.of(18, 0), LocalTime.of(17, 15),
                new BigDecimal("6.00"), LocalDate.of(2000, 1, 1)));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName()).isEqualTo("chk_period_end");
    }

    @Test
    void shouldThrowConstraintViolationWhenFeeNotGreaterThanZero() {
        courtPricingRepository.save(new CourtPricing(DayType.WEEKDAY, LocalTime.of(16, 30), LocalTime.of(17, 15),
                new BigDecimal("-3.00"), LocalDate.of(2000, 1, 1)));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName()).isEqualTo("chk_fee");
    }

    private void shouldFindApplicablePricing(final DayType dayType, final LocalTime time, final LocalDate date,
            final BigDecimal expectedFee) {
        final var retrieved = courtPricingRepository.findApplicablePricing(dayType, time, date);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getDayType()).isEqualTo(dayType);
        assertThat(retrieved.get().getFee()).isEqualByComparingTo(expectedFee);
    }
}
