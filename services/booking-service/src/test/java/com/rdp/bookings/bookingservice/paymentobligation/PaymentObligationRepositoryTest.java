package com.rdp.bookings.bookingservice.paymentobligation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import org.hibernate.exception.ConstraintViolationException;
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

import com.rdp.bookings.bookingservice.booking.Booking;
import com.rdp.bookings.bookingservice.booking.BookingStatus;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class PaymentObligationRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    private static Stream<Arguments> amountScenarios() {
        return Stream.of(Arguments.of("with an amount of £3.00", new BigDecimal("3.00")),
                Arguments.of("with an amount of £1.50", new BigDecimal("1.50")));
    }

    @Autowired
    PaymentObligationRepository paymentObligationRepository;

    @Autowired
    TestEntityManager entityManager;

    @ParameterizedTest(name = "{0}")
    @MethodSource("amountScenarios")
    void shouldSaveAndRetrievePaymentObligation(final String scenario, final BigDecimal amount) {
        final var booking = newBooking(new BigDecimal("6.00"));
        entityManager.persist(booking);
        entityManager.flush();
        entityManager.clear();

        final var memberId = UUID.randomUUID();
        final var status = PaymentObligationStatus.PENDING;
        final var paymentObligation = new PaymentObligation(booking, memberId, amount, status);
        paymentObligationRepository.save(paymentObligation);
        entityManager.flush();
        entityManager.clear();

        final var retrieved = paymentObligationRepository.findById(paymentObligation.getId());
        assertThat(retrieved).isPresent();
        final var found = retrieved.get();

        assertThat(found.getBooking().getId()).isEqualTo(booking.getId());
        assertThat(found.getMemberId()).isEqualTo(memberId);
        assertThat(found.getAmount()).isEqualByComparingTo(amount);
        assertThat(found.getStatus()).isEqualTo(status);
        assertThat(found.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldThrowConstraintViolationWhenAmountNotGreaterThanZero() {
        final var booking = newBooking(new BigDecimal("3.00"));
        entityManager.persist(booking);
        entityManager.flush();
        entityManager.clear();

        final var memberId = UUID.randomUUID();
        final var status = PaymentObligationStatus.PENDING;
        final var paymentObligation = new PaymentObligation(booking, memberId, new BigDecimal("-1.50"), status);

        paymentObligationRepository.save(paymentObligation);
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("chk_amount_positive");
    }

    private Booking newBooking(final BigDecimal courtFee) {
        final var courtId = UUID.randomUUID();
        final var timeSlotId = UUID.randomUUID();
        final var bookerMemberId = UUID.randomUUID();
        final var opponentMemberId = UUID.randomUUID();
        final var bookingDate = LocalDate.of(2000, 1, 1);
        final var status = BookingStatus.CONFIRMED;

        return new Booking(courtId, timeSlotId, bookerMemberId, opponentMemberId, bookingDate, status, courtFee);
    }
}
