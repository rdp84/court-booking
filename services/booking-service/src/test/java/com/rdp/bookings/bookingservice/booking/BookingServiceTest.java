package com.rdp.bookings.bookingservice.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.rdp.bookings.bookingservice.client.CourtPricingResponse;
import com.rdp.bookings.bookingservice.client.CourtResponse;
import com.rdp.bookings.bookingservice.client.CourtServiceClient;
import com.rdp.bookings.bookingservice.client.MemberResponse;
import com.rdp.bookings.bookingservice.client.MemberServiceClient;
import com.rdp.bookings.bookingservice.client.TimeSlotResponse;
import com.rdp.bookings.bookingservice.paymentobligation.PaymentObligationService;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    private static final UUID COURT_ID = UUID.randomUUID();
    private static final UUID TIME_SLOT_ID = UUID.randomUUID();
    private static final UUID BOOKER_ID = UUID.randomUUID();
    private static final UUID OPPONENT_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2030, 1, 6);
    private static final LocalDate BOOKING_DATE = TODAY.plusDays(1);
    private static final Duration FULL_REFUND_NOTICE = Duration.ofHours(24);
    private static final LocalTime SLOT_START = LocalTime.of(6, 45);
    private static final LocalTime SLOT_END = LocalTime.of(7, 30);
    private static final BigDecimal FEE = new BigDecimal("6.00");

    @Mock
    BookingRepository bookingRepository;

    @Mock
    CourtServiceClient courtServiceClient;

    @Mock
    MemberServiceClient memberServiceClient;

    @Mock
    PaymentObligationService paymentObligationService;

    BookingService bookingService;

    @BeforeEach
    void setup() {
        bookingService = serviceAt(TODAY.atTime(12, 0));
    }

    @Test
    void shouldGetBookingById() {
        final var id = UUID.randomUUID();
        final var booking = new Booking(COURT_ID, TIME_SLOT_ID, SLOT_START, SLOT_END, BOOKER_ID, null, BOOKING_DATE,
                BookingStatus.CONFIRMED, FEE);
        given(bookingRepository.findById(id)).willReturn(Optional.of(booking));

        assertThat(bookingService.getBookingById(id)).containsSame(booking);
    }

    @Nested
    class SuccessfulBooking {

        private static Stream<Arguments> feeSplitScenarios() {
            return Stream.of(Arguments.of("an even fee", new BigDecimal("6.00"), new BigDecimal("3.00")),
                    Arguments.of("a fee split to the half penny", new BigDecimal("7.50"), new BigDecimal("3.75")),
                    Arguments.of("an odd penny rounded up", new BigDecimal("7.25"), new BigDecimal("3.63")));
        }

        private static Stream<Arguments> membershipBoundaryScenarios() {
            return Stream.of(Arguments.of("membership starting on the booking date", BOOKING_DATE,
                    BOOKING_DATE.plusYears(1)),
                    Arguments.of("membership ending on the booking date", BOOKING_DATE.minusYears(1), BOOKING_DATE));
        }

        @Test
        void shouldCreateBookingWithGuestOpponent() {
            givenBookableCourtSlotWithFee(FEE);
            givenActiveMember(BOOKER_ID);
            givenSlotFreeAndNoOverlaps();

            final var booking = bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null);

            assertThat(booking.getCourtId()).isEqualTo(COURT_ID);
            assertThat(booking.getTimeSlotId()).isEqualTo(TIME_SLOT_ID);
            assertThat(booking.getSlotStart()).isEqualTo(SLOT_START);
            assertThat(booking.getSlotEnd()).isEqualTo(SLOT_END);
            assertThat(booking.getBookerMemberId()).isEqualTo(BOOKER_ID);
            assertThat(booking.getOpponentMemberId()).isNull();
            assertThat(booking.getBookingDate()).isEqualTo(BOOKING_DATE);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(booking.getCourtFee()).isEqualByComparingTo(FEE);
            verifyNoInteractions(paymentObligationService);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("feeSplitScenarios")
        void shouldCreatePaymentObligationForHalfTheFeeWithMemberOpponent(final String scenario,
                final BigDecimal fee, final BigDecimal expectedShare) {
            givenBookableCourtSlotWithFee(fee);
            givenActiveMember(BOOKER_ID);
            givenActiveMember(OPPONENT_ID);
            givenSlotFreeAndNoOverlaps();

            final var booking = bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID,
                    OPPONENT_ID);

            assertThat(booking.getOpponentMemberId()).isEqualTo(OPPONENT_ID);
            assertThat(booking.getCourtFee()).isEqualByComparingTo(fee);
            verify(paymentObligationService).createPendingObligation(booking, OPPONENT_ID, expectedShare);
        }

        @Test
        void shouldAllowBookingForToday() {
            final var today = TODAY;
            givenBookableCourtSlotWithFee(FEE, today);
            given(memberServiceClient.getMember(BOOKER_ID)).willReturn(Optional.of(
                    new MemberResponse(BOOKER_ID, BigDecimal.ZERO, today.minusYears(1), today.plusYears(1))));
            givenSlotFreeAndNoOverlaps();

            final var booking = bookingService.createBooking(COURT_ID, TIME_SLOT_ID, today, BOOKER_ID, null);

            assertThat(booking.getBookingDate()).isEqualTo(today);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("membershipBoundaryScenarios")
        void shouldAllowBookingOnMembershipBoundary(final String scenario, final LocalDate membershipStart,
                final LocalDate membershipEnd) {
            givenBookableCourtSlotWithFee(FEE);
            given(memberServiceClient.getMember(BOOKER_ID)).willReturn(
                    Optional.of(new MemberResponse(BOOKER_ID, BigDecimal.ZERO, membershipStart, membershipEnd)));
            givenSlotFreeAndNoOverlaps();

            final var booking = bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null);

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }
    }

    @Nested
    class RejectedBooking {

        private static Stream<Arguments> inactiveMembershipScenarios() {
            return Stream.of(Arguments.of("membership starting the day after the booking date",
                    BOOKING_DATE.plusDays(1), BOOKING_DATE.plusYears(1)),
                    Arguments.of("membership ending the day before the booking date", BOOKING_DATE.minusYears(1),
                            BOOKING_DATE.minusDays(1)));
        }

        @Test
        void shouldRejectBookingDateInThePast() {
            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, TODAY.minusDays(1),
                    BOOKER_ID, null), RejectionReason.BOOKING_DATE_IN_PAST);
            verifyNoInteractions(courtServiceClient, memberServiceClient, bookingRepository);
        }

        @Test
        void shouldRejectOpponentWhoIsTheBooker() {
            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID,
                    BOOKER_ID), RejectionReason.OPPONENT_IS_BOOKER);
            verifyNoInteractions(courtServiceClient, memberServiceClient, bookingRepository);
        }

        @Test
        void shouldRejectUnknownCourt() {
            given(courtServiceClient.getCourt(COURT_ID)).willReturn(Optional.empty());

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.COURT_NOT_FOUND);
        }

        @Test
        void shouldRejectInactiveCourt() {
            given(courtServiceClient.getCourt(COURT_ID))
                    .willReturn(Optional.of(new CourtResponse(COURT_ID, "Court 1", false)));

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.COURT_INACTIVE);
        }

        @Test
        void shouldRejectUnknownTimeSlot() {
            givenActiveCourt();
            given(courtServiceClient.getTimeSlot(TIME_SLOT_ID)).willReturn(Optional.empty());

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.TIME_SLOT_NOT_FOUND);
        }

        @Test
        void shouldRejectTimeSlotOnAnotherCourt() {
            givenActiveCourt();
            given(courtServiceClient.getTimeSlot(TIME_SLOT_ID)).willReturn(
                    Optional.of(new TimeSlotResponse(TIME_SLOT_ID, UUID.randomUUID(), SLOT_START, SLOT_END)));

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.TIME_SLOT_NOT_ON_COURT);
        }

        @Test
        void shouldRejectSlotWithNoPricing() {
            givenActiveCourt();
            givenTimeSlot();
            given(courtServiceClient.getPricing(BOOKING_DATE, SLOT_START)).willReturn(Optional.empty());

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.NO_PRICING);
        }

        @Test
        void shouldRejectUnknownBooker() {
            givenBookableCourtSlotWithFee(FEE);
            given(memberServiceClient.getMember(BOOKER_ID)).willReturn(Optional.empty());

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.MEMBER_NOT_FOUND);
        }

        @Test
        void shouldRejectUnknownOpponent() {
            givenBookableCourtSlotWithFee(FEE);
            givenActiveMember(BOOKER_ID);
            given(memberServiceClient.getMember(OPPONENT_ID)).willReturn(Optional.empty());

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID,
                    OPPONENT_ID), RejectionReason.MEMBER_NOT_FOUND);
        }

        @ParameterizedTest(name = "booker with {0}")
        @MethodSource("inactiveMembershipScenarios")
        void shouldRejectBookerWithInactiveMembership(final String scenario, final LocalDate membershipStart,
                final LocalDate membershipEnd) {
            givenBookableCourtSlotWithFee(FEE);
            given(memberServiceClient.getMember(BOOKER_ID)).willReturn(
                    Optional.of(new MemberResponse(BOOKER_ID, BigDecimal.ZERO, membershipStart, membershipEnd)));

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.MEMBERSHIP_INACTIVE);
        }

        @ParameterizedTest(name = "opponent with {0}")
        @MethodSource("inactiveMembershipScenarios")
        void shouldRejectOpponentWithInactiveMembership(final String scenario, final LocalDate membershipStart,
                final LocalDate membershipEnd) {
            givenBookableCourtSlotWithFee(FEE);
            givenActiveMember(BOOKER_ID);
            given(memberServiceClient.getMember(OPPONENT_ID)).willReturn(
                    Optional.of(new MemberResponse(OPPONENT_ID, BigDecimal.ZERO, membershipStart, membershipEnd)));

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID,
                    OPPONENT_ID), RejectionReason.MEMBERSHIP_INACTIVE);
        }

        @Test
        void shouldRejectCourtSlotAlreadyBooked() {
            givenBookableCourtSlotWithFee(FEE);
            givenActiveMember(BOOKER_ID);
            given(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(COURT_ID, TIME_SLOT_ID,
                    BOOKING_DATE, BookingStatus.CONFIRMED)).willReturn(true);

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.SLOT_ALREADY_BOOKED);
        }

        @Test
        void shouldRejectBookerWithOverlappingBooking() {
            givenBookableCourtSlotWithFee(FEE);
            givenActiveMember(BOOKER_ID);
            given(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(COURT_ID, TIME_SLOT_ID,
                    BOOKING_DATE, BookingStatus.CONFIRMED)).willReturn(false);
            given(bookingRepository.existsOverlappingBooking(BOOKER_ID, BOOKING_DATE, SLOT_START, SLOT_END))
                    .willReturn(true);

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null),
                    RejectionReason.MEMBER_HAS_OVERLAPPING_BOOKING);
        }

        @Test
        void shouldRejectOpponentWithOverlappingBooking() {
            givenBookableCourtSlotWithFee(FEE);
            givenActiveMember(BOOKER_ID);
            givenActiveMember(OPPONENT_ID);
            given(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(COURT_ID, TIME_SLOT_ID,
                    BOOKING_DATE, BookingStatus.CONFIRMED)).willReturn(false);
            given(bookingRepository.existsOverlappingBooking(BOOKER_ID, BOOKING_DATE, SLOT_START, SLOT_END))
                    .willReturn(false);
            given(bookingRepository.existsOverlappingBooking(OPPONENT_ID, BOOKING_DATE, SLOT_START, SLOT_END))
                    .willReturn(true);

            assertRejected(() -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID,
                    OPPONENT_ID), RejectionReason.MEMBER_HAS_OVERLAPPING_BOOKING);
        }

        private void assertRejected(final ThrowingCallable createBooking, final RejectionReason expectedReason) {
            assertThatThrownBy(createBooking).isInstanceOf(BookingRejectedException.class)
                    .extracting(e -> ((BookingRejectedException) e).getReason()).isEqualTo(expectedReason);
            verify(bookingRepository, never()).saveAndFlush(any());
            verifyNoInteractions(paymentObligationService);
        }
    }

    @Nested
    class ConcurrentBooking {

        private static Stream<Arguments> constraintScenarios() {
            return Stream.of(Arguments.of("uq_court_slot_date_active", RejectionReason.SLOT_ALREADY_BOOKED),
                    Arguments.of("uq_member_slot_date_active", RejectionReason.MEMBER_HAS_OVERLAPPING_BOOKING));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("constraintScenarios")
        void shouldRejectWhenUniqueIndexViolatedOnSave(final String constraintName,
                final RejectionReason expectedReason) {
            givenValidRequestWithGuestOpponent();
            given(bookingRepository.saveAndFlush(any(Booking.class)))
                    .willThrow(constraintViolation(constraintName));

            assertThatThrownBy(
                    () -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null))
                    .isInstanceOf(BookingRejectedException.class)
                    .extracting(e -> ((BookingRejectedException) e).getReason()).isEqualTo(expectedReason);
        }

        @Test
        void shouldRethrowOtherDataIntegrityViolations() {
            givenValidRequestWithGuestOpponent();
            final var violation = constraintViolation("chk_court_fee_positive");
            given(bookingRepository.saveAndFlush(any(Booking.class))).willThrow(violation);

            assertThatThrownBy(
                    () -> bookingService.createBooking(COURT_ID, TIME_SLOT_ID, BOOKING_DATE, BOOKER_ID, null))
                    .isSameAs(violation);
        }

        private void givenValidRequestWithGuestOpponent() {
            givenActiveCourt();
            givenTimeSlot();
            given(courtServiceClient.getPricing(BOOKING_DATE, SLOT_START))
                    .willReturn(Optional.of(new CourtPricingResponse(FEE)));
            givenActiveMember(BOOKER_ID);
            given(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(COURT_ID, TIME_SLOT_ID,
                    BOOKING_DATE, BookingStatus.CONFIRMED)).willReturn(false);
            given(bookingRepository.existsOverlappingBooking(BOOKER_ID, BOOKING_DATE, SLOT_START, SLOT_END))
                    .willReturn(false);
        }

        private DataIntegrityViolationException constraintViolation(final String constraintName) {
            return new DataIntegrityViolationException("could not execute statement",
                    new ConstraintViolationException("duplicate key", null, constraintName));
        }
    }

    @Nested
    class CancelBooking {
        private static final UUID BOOKING_ID = UUID.randomUUID();
        private static final LocalDateTime SLOT_STARTS_AT = BOOKING_DATE.atTime(SLOT_START);

        private static Stream<Arguments> refundScenarios() {
            return Stream.of(Arguments.of("two days before", SLOT_STARTS_AT.minusDays(2),
                    BookingStatus.CANCELLED_FULL_REFUND),
                    Arguments.of("exactly the full refund notice before", SLOT_STARTS_AT.minus(FULL_REFUND_NOTICE),
                            BookingStatus.CANCELLED_FULL_REFUND),
                    Arguments.of("a minute inside the full refund notice",
                            SLOT_STARTS_AT.minus(FULL_REFUND_NOTICE).plusMinutes(1),
                            BookingStatus.CANCELLED_NO_REFUND),
                    Arguments.of("a minute before the slot starts", SLOT_STARTS_AT.minusMinutes(1),
                            BookingStatus.CANCELLED_NO_REFUND));
        }

        private static Stream<Arguments> startedScenarios() {
            return Stream.of(Arguments.of("as the slot starts", SLOT_STARTS_AT),
                    Arguments.of("after the slot has started", SLOT_STARTS_AT.plusMinutes(15)),
                    Arguments.of("on a later day", SLOT_STARTS_AT.plusDays(1)));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("refundScenarios")
        void shouldCancelWithRefundStatusBasedOnNotice(final String scenario, final LocalDateTime now,
                final BookingStatus expectedStatus) {
            final var booking = givenConfirmedBooking();
            given(bookingRepository.save(booking)).willReturn(booking);

            final var cancelled = serviceAt(now).cancelBooking(BOOKING_ID, BOOKER_ID);

            assertThat(cancelled.getStatus()).isEqualTo(expectedStatus);
            assertThat(cancelled.getCancelledAt()).isEqualTo(now);
        }

        @Test
        void shouldWaivePendingObligationsWhenFullyRefunded() {
            final var booking = givenConfirmedBooking();
            given(bookingRepository.save(booking)).willReturn(booking);

            serviceAt(SLOT_STARTS_AT.minusDays(2)).cancelBooking(BOOKING_ID, BOOKER_ID);

            verify(paymentObligationService).waivePendingObligations(booking);
        }

        @Test
        void shouldKeepPendingObligationsWhenNotRefunded() {
            final var booking = givenConfirmedBooking();
            given(bookingRepository.save(booking)).willReturn(booking);

            serviceAt(SLOT_STARTS_AT.minusHours(1)).cancelBooking(BOOKING_ID, BOOKER_ID);

            verifyNoInteractions(paymentObligationService);
        }

        @Test
        void shouldRejectUnknownBooking() {
            given(bookingRepository.findById(BOOKING_ID)).willReturn(Optional.empty());

            assertCancelRejected(bookingService, BOOKER_ID, RejectionReason.BOOKING_NOT_FOUND);
        }

        @Test
        void shouldRejectMemberWhoIsNotTheBooker() {
            givenConfirmedBooking();

            assertCancelRejected(bookingService, OPPONENT_ID, RejectionReason.NOT_BOOKER);
        }

        @Test
        void shouldRejectBookingAlreadyCancelled() {
            final var booking = givenConfirmedBooking();
            booking.setStatus(BookingStatus.CANCELLED_FULL_REFUND);
            booking.setCancelledAt(TODAY.atTime(9, 0));

            assertCancelRejected(bookingService, BOOKER_ID, RejectionReason.ALREADY_CANCELLED);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("startedScenarios")
        void shouldRejectBookingThatHasStarted(final String scenario, final LocalDateTime now) {
            givenConfirmedBooking();

            assertCancelRejected(serviceAt(now), BOOKER_ID, RejectionReason.BOOKING_ALREADY_STARTED);
        }

        private Booking givenConfirmedBooking() {
            final var booking = new Booking(COURT_ID, TIME_SLOT_ID, SLOT_START, SLOT_END, BOOKER_ID, OPPONENT_ID,
                    BOOKING_DATE, BookingStatus.CONFIRMED, FEE);
            given(bookingRepository.findById(BOOKING_ID)).willReturn(Optional.of(booking));
            return booking;
        }

        private void assertCancelRejected(final BookingService service, final UUID memberId,
                final RejectionReason expectedReason) {
            assertThatThrownBy(() -> service.cancelBooking(BOOKING_ID, memberId))
                    .isInstanceOf(BookingRejectedException.class)
                    .extracting(e -> ((BookingRejectedException) e).getReason()).isEqualTo(expectedReason);
            verify(bookingRepository, never()).save(any());
            verifyNoInteractions(paymentObligationService);
        }
    }

    private BookingService serviceAt(final LocalDateTime now) {
        final var zone = ZoneId.systemDefault();
        return new BookingService(bookingRepository, courtServiceClient, memberServiceClient,
                paymentObligationService, new BookingProperties(FULL_REFUND_NOTICE),
                Clock.fixed(now.atZone(zone).toInstant(), zone));
    }

    private void givenActiveCourt() {
        given(courtServiceClient.getCourt(COURT_ID)).willReturn(Optional.of(new CourtResponse(COURT_ID, "Court 1",
                true)));
    }

    private void givenTimeSlot() {
        given(courtServiceClient.getTimeSlot(TIME_SLOT_ID))
                .willReturn(Optional.of(new TimeSlotResponse(TIME_SLOT_ID, COURT_ID, SLOT_START, SLOT_END)));
    }

    private void givenBookableCourtSlotWithFee(final BigDecimal fee) {
        givenBookableCourtSlotWithFee(fee, BOOKING_DATE);
    }

    private void givenBookableCourtSlotWithFee(final BigDecimal fee, final LocalDate bookingDate) {
        givenActiveCourt();
        givenTimeSlot();
        given(courtServiceClient.getPricing(bookingDate, SLOT_START))
                .willReturn(Optional.of(new CourtPricingResponse(fee)));
    }

    private void givenActiveMember(final UUID memberId) {
        given(memberServiceClient.getMember(memberId)).willReturn(Optional.of(
                new MemberResponse(memberId, BigDecimal.ZERO, BOOKING_DATE.minusYears(1), BOOKING_DATE.plusYears(1))));
    }

    private void givenSlotFreeAndNoOverlaps() {
        given(bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(eq(COURT_ID), eq(TIME_SLOT_ID),
                any(LocalDate.class), eq(BookingStatus.CONFIRMED))).willReturn(false);
        given(bookingRepository.existsOverlappingBooking(any(UUID.class), any(LocalDate.class), eq(SLOT_START),
                eq(SLOT_END))).willReturn(false);
        given(bookingRepository.saveAndFlush(any(Booking.class))).willAnswer(invocation -> invocation.getArgument(0));
    }
}
