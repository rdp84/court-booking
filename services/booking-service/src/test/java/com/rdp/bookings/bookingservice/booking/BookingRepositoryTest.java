package com.rdp.bookings.bookingservice.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final LocalTime SLOT_START = LocalTime.of(6, 45);
    private static final LocalTime SLOT_END = LocalTime.of(7, 30);
    private static final LocalDate BOOKING_DATE = LocalDate.of(2000, 1, 1);

    private static Stream<Arguments> opponentScenarios() {
        return Stream.of(Arguments.of("with a member opponent", UUID.randomUUID()),
                Arguments.of("with a guest opponent", null));
    }

    private static Stream<Arguments> cancelledAtConsistencyScenarios() {
        return Stream.of(Arguments.of("with cancelled full refund", BookingStatus.CANCELLED_FULL_REFUND),
                Arguments.of("with cancelled no refund", BookingStatus.CANCELLED_NO_REFUND));
    }

    // Each scenario is checked against an existing booking for SLOT_START - SLOT_END (06:45 - 07:30)
    private static Stream<Arguments> overlapScenarios() {
        return Stream.of(Arguments.of("overlapping the end", LocalTime.of(7, 15), LocalTime.of(8, 0), true),
                Arguments.of("overlapping the start", LocalTime.of(6, 15), LocalTime.of(7, 0), true),
                Arguments.of("the identical slot", SLOT_START, SLOT_END, true),
                Arguments.of("contained within it", LocalTime.of(7, 0), LocalTime.of(7, 15), true),
                Arguments.of("containing it", LocalTime.of(6, 30), LocalTime.of(8, 0), true),
                Arguments.of("starting as it ends", LocalTime.of(7, 30), LocalTime.of(8, 15), false),
                Arguments.of("ending as it starts", LocalTime.of(6, 0), LocalTime.of(6, 45), false));
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
        assertThat(found.getSlotStart()).isEqualTo(SLOT_START);
        assertThat(found.getSlotEnd()).isEqualTo(SLOT_END);
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

        bookingRepository.save(new Booking(courtId, timeSlotId, SLOT_START, SLOT_END, UUID.randomUUID(),
                UUID.randomUUID(), bookingDate, status, courtFee));
        entityManager.flush();
        entityManager.clear();

        bookingRepository.save(new Booking(courtId, timeSlotId, SLOT_START, SLOT_END, UUID.randomUUID(), null,
                bookingDate, status, courtFee));
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

        bookingRepository.save(new Booking(UUID.randomUUID(), timeSlotId, SLOT_START, SLOT_END, bookerMemberId,
                UUID.randomUUID(), bookingDate, status, courtFee));
        entityManager.flush();
        entityManager.clear();

        bookingRepository.save(new Booking(UUID.randomUUID(), timeSlotId, SLOT_START, SLOT_END, bookerMemberId, null,
                bookingDate, status, courtFee));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("uq_member_slot_date_active");
    }

    // BookingService relies on this shape to map a concurrent double booking to a rejection
    @Test
    void shouldExposeIndexNameWhenSaveAndFlushViolatesUniqueIndex() {
        final var existing = saveConfirmedBooking(UUID.randomUUID(), null);

        assertThatThrownBy(() -> bookingRepository.saveAndFlush(new Booking(existing.getCourtId(),
                existing.getTimeSlotId(), SLOT_START, SLOT_END, UUID.randomUUID(), null, BOOKING_DATE,
                BookingStatus.CONFIRMED, new BigDecimal("6.00")))).isInstanceOf(DataIntegrityViolationException.class)
                .cause().isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("uq_court_slot_date_active");
    }

    @Test
    void shouldRebookCourtThatIsCancelled() {
        final var courtId = UUID.randomUUID();
        final var timeSlotId = UUID.randomUUID();
        final var bookingDate = LocalDate.of(2000, 1, 1);
        final var courtFee = new BigDecimal("6.00");

        final var first = new Booking(courtId, timeSlotId, SLOT_START, SLOT_END, UUID.randomUUID(), null, bookingDate,
                BookingStatus.CONFIRMED, courtFee);
        bookingRepository.save(first);
        entityManager.flush();
        entityManager.clear();

        first.setStatus(BookingStatus.CANCELLED_FULL_REFUND);
        first.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(first);
        entityManager.flush();
        entityManager.clear();

        bookingRepository.save(new Booking(courtId, timeSlotId, SLOT_START, SLOT_END, UUID.randomUUID(),
                UUID.randomUUID(), bookingDate, BookingStatus.CONFIRMED, courtFee));
        assertThatNoException().isThrownBy(() -> entityManager.flush());
    }

    @Test
    void shouldFindConfirmedBookingForCourtSlotAndDate() {
        final var booking = saveConfirmedBooking(UUID.randomUUID(), null);

        assertThat(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(booking.getCourtId(),
                booking.getTimeSlotId(), BOOKING_DATE, BookingStatus.CONFIRMED)).isTrue();
        assertThat(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(booking.getCourtId(),
                booking.getTimeSlotId(), BOOKING_DATE.plusDays(1), BookingStatus.CONFIRMED)).isFalse();
    }

    @Test
    void shouldNotFindCancelledBookingForCourtSlotAndDate() {
        final var booking = saveCancelledBooking(UUID.randomUUID());

        assertThat(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(booking.getCourtId(),
                booking.getTimeSlotId(), BOOKING_DATE, BookingStatus.CONFIRMED)).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("overlapScenarios")
    void shouldDetectOverlapWithBookersExistingBooking(final String scenario, final LocalTime slotStart,
            final LocalTime slotEnd, final boolean expected) {
        final var memberId = UUID.randomUUID();
        saveConfirmedBooking(memberId, null);

        assertThat(bookingRepository.existsOverlappingBooking(memberId, BOOKING_DATE, slotStart, slotEnd))
                .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("overlapScenarios")
    void shouldDetectOverlapWithOpponentsExistingBooking(final String scenario, final LocalTime slotStart,
            final LocalTime slotEnd, final boolean expected) {
        final var memberId = UUID.randomUUID();
        saveConfirmedBooking(UUID.randomUUID(), memberId);

        assertThat(bookingRepository.existsOverlappingBooking(memberId, BOOKING_DATE, slotStart, slotEnd))
                .isEqualTo(expected);
    }

    @Test
    void shouldNotDetectOverlapOnDifferentDate() {
        final var memberId = UUID.randomUUID();
        saveConfirmedBooking(memberId, null);

        assertThat(bookingRepository.existsOverlappingBooking(memberId, BOOKING_DATE.plusDays(1), SLOT_START,
                SLOT_END)).isFalse();
    }

    @Test
    void shouldNotDetectOverlapForDifferentMember() {
        saveConfirmedBooking(UUID.randomUUID(), UUID.randomUUID());

        assertThat(bookingRepository.existsOverlappingBooking(UUID.randomUUID(), BOOKING_DATE, SLOT_START, SLOT_END))
                .isFalse();
    }

    @Test
    void shouldNotDetectOverlapWithCancelledBooking() {
        final var memberId = UUID.randomUUID();
        saveCancelledBooking(memberId);

        assertThat(bookingRepository.existsOverlappingBooking(memberId, BOOKING_DATE, SLOT_START, SLOT_END))
                .isFalse();
    }

    private Booking saveConfirmedBooking(final UUID bookerMemberId, final UUID opponentMemberId) {
        final var booking = bookingRepository.save(new Booking(UUID.randomUUID(), UUID.randomUUID(), SLOT_START,
                SLOT_END, bookerMemberId, opponentMemberId, BOOKING_DATE, BookingStatus.CONFIRMED,
                new BigDecimal("6.00")));
        entityManager.flush();
        entityManager.clear();
        return booking;
    }

    private Booking saveCancelledBooking(final UUID bookerMemberId) {
        final var booking = new Booking(UUID.randomUUID(), UUID.randomUUID(), SLOT_START, SLOT_END, bookerMemberId,
                null, BOOKING_DATE, BookingStatus.CANCELLED_FULL_REFUND, new BigDecimal("6.00"));
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
        entityManager.flush();
        entityManager.clear();
        return booking;
    }

    private Booking newBooking(final UUID opponentMemberId, final BigDecimal courtFee) {
        final var courtId = UUID.randomUUID();
        final var timeSlotId = UUID.randomUUID();
        final var bookerMemberId = UUID.randomUUID();
        final var bookingDate = LocalDate.of(2000, 1, 1);
        final var status = BookingStatus.CONFIRMED;

        return new Booking(courtId, timeSlotId, SLOT_START, SLOT_END, bookerMemberId, opponentMemberId, bookingDate,
                status, courtFee);
    }

}
