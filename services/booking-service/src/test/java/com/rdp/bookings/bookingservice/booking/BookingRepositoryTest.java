package com.rdp.bookings.bookingservice.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
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

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class BookingRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    private static Stream<Arguments> opponentScenarios() {
        return Stream.of(Arguments.of("with a member opponent", UUID.randomUUID()),
                Arguments.of("with a guest opponent", null));
    }

    private static Stream<Arguments> cancelledAtConsistencyScenarios() {
        return Stream.of(Arguments.of("with cancelled full refund", BookingStatus.CANCELLED_FULL_REFUND),
                Arguments.of("with cancelled no refund", BookingStatus.CANCELLED_NO_REFUND));
    }

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    TestEntityManager entityManager;

    @ParameterizedTest(name = "{0}")
    @MethodSource("opponentScenarios")
    void shouldSaveAndRetrieveBooking(final String scenario, final UUID opponentMemberId) {
        final var booking = newBooking(opponentMemberId, new BigDecimal("6.00"));

        bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear();

        final var retrieved = bookingRepository.findById(booking.getId());
        assertThat(retrieved).isPresent();

        final var found = retrieved.get();
        assertThat(found.getCourtId()).isEqualTo(booking.getCourtId());
        assertThat(found.getTimeSlotId()).isEqualTo(booking.getTimeSlotId());
        assertThat(found.getBookerMemberId()).isEqualTo(booking.getBookerMemberId());
        assertThat(found.getOpponentMemberId()).isEqualTo(opponentMemberId);
        assertThat(found.getBookingDate()).isEqualTo(booking.getBookingDate());
        assertThat(found.getStatus()).isEqualTo(booking.getStatus());
        assertThat(found.getCourtFee()).isEqualByComparingTo(booking.getCourtFee());
    }

    @Test
    void shouldThrowConstraintViolationWhenCourtFeeNotGreaterThanZero() {
        final var booking = newBooking(UUID.randomUUID(), new BigDecimal("-6.00"));

        bookingRepository.save(booking);
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("chk_court_fee_positive");
    }

    @Test
    void shouldThrowConstraintViolationWhenConfirmedBookingHasCancelledAt() {
        final var booking = newBooking(null, new BigDecimal("3.00"));
        booking.setCancelledAt(LocalDateTime.now());

        bookingRepository.save(booking);
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("chk_cancelled_at_consistency");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cancelledAtConsistencyScenarios")
    void shouldThrowConstraintViolationWhenCancelledAndNoCancelledAt(final String scenario,
            final BookingStatus status) {
        final var booking = newBooking(UUID.randomUUID(), new BigDecimal("6.00"));
        booking.setStatus(status);

        bookingRepository.save(booking);
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("chk_cancelled_at_consistency");
    }

    @Test
    void shouldThrowConstraintViolationWhenSameCourtSlotDateBookedTwice() {
        final var courtId = UUID.randomUUID();
        final var timeSlotId = UUID.randomUUID();
        final var bookingDate = LocalDate.of(2000, 1, 1);
        final var status = BookingStatus.CONFIRMED;
        final var courtFee = new BigDecimal("6.00");

        bookingRepository.save(
                new Booking(courtId, timeSlotId, UUID.randomUUID(), UUID.randomUUID(), bookingDate, status, courtFee));
        entityManager.flush();
        entityManager.clear();

        bookingRepository
                .save(new Booking(courtId, timeSlotId, UUID.randomUUID(), null, bookingDate, status, courtFee));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("uq_court_slot_date_active");
    }

    @Test
    void shouldThrowConstraintViolationWhenSameMemberBooksSameSlotTwice() {
        final var timeSlotId = UUID.randomUUID();
        final var bookerMemberId = UUID.randomUUID();
        final var bookingDate = LocalDate.of(2000, 1, 1);
        final var status = BookingStatus.CONFIRMED;
        final var courtFee = new BigDecimal("3.00");

        bookingRepository.save(
                new Booking(UUID.randomUUID(), timeSlotId, bookerMemberId, UUID.randomUUID(), bookingDate, status,
                        courtFee));
        entityManager.flush();
        entityManager.clear();

        bookingRepository.save(
                new Booking(UUID.randomUUID(), timeSlotId, bookerMemberId, null, bookingDate, status, courtFee));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("uq_member_slot_date_active");
    }

    @Test
    void shouldRebookCourtThatIsCancelled() {
        final var courtId = UUID.randomUUID();
        final var timeSlotId = UUID.randomUUID();
        final var bookingDate = LocalDate.of(2000, 1, 1);
        final var courtFee = new BigDecimal("6.00");

        final var first = new Booking(courtId, timeSlotId, UUID.randomUUID(), null, bookingDate,
                BookingStatus.CONFIRMED, courtFee);
        bookingRepository.save(first);
        entityManager.flush();
        entityManager.clear();

        first.setStatus(BookingStatus.CANCELLED_FULL_REFUND);
        first.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(first);
        entityManager.flush();
        entityManager.clear();

        bookingRepository.save(new Booking(courtId, timeSlotId, UUID.randomUUID(), UUID.randomUUID(), bookingDate,
                BookingStatus.CONFIRMED, courtFee));
        assertThatNoException().isThrownBy(() -> entityManager.flush());
    }

    private Booking newBooking(final UUID opponentMemberId, final BigDecimal courtFee) {
        final var courtId = UUID.randomUUID();
        final var timeSlotId = UUID.randomUUID();
        final var bookerMemberId = UUID.randomUUID();
        final var bookingDate = LocalDate.of(2000, 1, 1);
        final var status = BookingStatus.CONFIRMED;

        return new Booking(courtId, timeSlotId, bookerMemberId, opponentMemberId, bookingDate, status, courtFee);
    }

}
