package com.rdp.courts.courtservice.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
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

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    CourtPricingRepository courtPricingRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveCourtPricing() {
        final var courtPricing = new CourtPricing(DayType.WEEKDAY, LocalTime.of(12, 45), LocalTime.of(13, 30),
                new BigDecimal("3.00"), LocalDate.of(2000, 1, 1));
        courtPricingRepository.save(courtPricing);
        entityManager.flush();
        entityManager.clear();

        final var retrieved = courtPricingRepository.findById(courtPricing.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isNotNull();
        assertThat(retrieved.get().getDayType()).isEqualTo(DayType.WEEKDAY);
        assertThat(retrieved.get().getPeriodStart()).isEqualTo(LocalTime.of(12, 45));
        assertThat(retrieved.get().getPeriodEnd()).isEqualTo(LocalTime.of(13, 30));
        assertThat(retrieved.get().getFee()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(retrieved.get().getValidFrom()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(retrieved.get().getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldThrowConstraintViolationWhenDuplicateDayStartValidFrom() {
        courtPricingRepository.save(new CourtPricing(DayType.WEEKEND, LocalTime.of(8, 45), LocalTime.of(9, 30),
                new BigDecimal("3.00"), LocalDate.of(2000, 1, 1)));
        entityManager.flush();
        entityManager.clear();

        courtPricingRepository.save(new CourtPricing(DayType.WEEKEND, LocalTime.of(8, 45), LocalTime.of(9, 30),
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
}
