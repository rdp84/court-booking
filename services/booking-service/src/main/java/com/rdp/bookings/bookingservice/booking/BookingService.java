package com.rdp.bookings.bookingservice.booking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.bookings.bookingservice.client.CourtServiceClient;
import com.rdp.bookings.bookingservice.client.MemberServiceClient;
import com.rdp.bookings.bookingservice.client.TimeSlotResponse;
import com.rdp.bookings.bookingservice.paymentobligation.PaymentObligationService;

@Service
class BookingService {
    private static final String COURT_SLOT_INDEX = "uq_court_slot_date_active";
    private static final String MEMBER_SLOT_INDEX = "uq_member_slot_date_active";

    private final BookingRepository bookingRepository;
    private final CourtServiceClient courtServiceClient;
    private final MemberServiceClient memberServiceClient;
    private final PaymentObligationService paymentObligationService;
    private final BookingProperties properties;
    private final Clock clock;

    BookingService(final BookingRepository bookingRepository, final CourtServiceClient courtServiceClient,
            final MemberServiceClient memberServiceClient, final PaymentObligationService paymentObligationService,
            final BookingProperties properties, final Clock clock) {
        this.bookingRepository = bookingRepository;
        this.courtServiceClient = courtServiceClient;
        this.memberServiceClient = memberServiceClient;
        this.paymentObligationService = paymentObligationService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    Booking createBooking(final UUID courtId, final UUID timeSlotId, final LocalDate bookingDate,
            final UUID bookerMemberId, final UUID opponentMemberId) {
        if (bookingDate.isBefore(LocalDate.now(clock))) {
            throw new BookingRejectedException(RejectionReason.BOOKING_DATE_IN_PAST,
                    "Booking date is in the past: " + bookingDate);
        }
        if (bookerMemberId.equals(opponentMemberId)) {
            throw new BookingRejectedException(RejectionReason.OPPONENT_IS_BOOKER,
                    "Member cannot book against themselves: " + bookerMemberId);
        }

        final var court = courtServiceClient.getCourt(courtId)
                .orElseThrow(() -> new BookingRejectedException(RejectionReason.COURT_NOT_FOUND,
                        "Court not found: " + courtId));
        if (!court.isActive()) {
            throw new BookingRejectedException(RejectionReason.COURT_INACTIVE, "Court is not active: " + courtId);
        }

        final var slot = courtServiceClient.getTimeSlot(timeSlotId)
                .orElseThrow(() -> new BookingRejectedException(RejectionReason.TIME_SLOT_NOT_FOUND,
                        "Time slot not found: " + timeSlotId));
        if (!slot.courtId().equals(courtId)) {
            throw new BookingRejectedException(RejectionReason.TIME_SLOT_NOT_ON_COURT,
                    "Time slot " + timeSlotId + " does not belong to court " + courtId);
        }

        final var fee = courtServiceClient.getPricing(bookingDate, slot.slotStart())
                .orElseThrow(() -> new BookingRejectedException(RejectionReason.NO_PRICING,
                        "No pricing for " + bookingDate + " at " + slot.slotStart()))
                .fee();

        requireActiveMember(bookerMemberId, bookingDate);
        if (opponentMemberId != null) {
            requireActiveMember(opponentMemberId, bookingDate);
        }

        if (bookingRepository.existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(courtId, timeSlotId, bookingDate,
                BookingStatus.CONFIRMED)) {
            throw new BookingRejectedException(RejectionReason.SLOT_ALREADY_BOOKED,
                    "Court " + courtId + " is already booked for slot " + timeSlotId + " on " + bookingDate);
        }
        requireNoOverlappingBooking(bookerMemberId, bookingDate, slot);
        if (opponentMemberId != null) {
            requireNoOverlappingBooking(opponentMemberId, bookingDate, slot);
        }

        final var booking = save(new Booking(courtId, timeSlotId, slot.slotStart(), slot.slotEnd(), bookerMemberId,
                opponentMemberId, bookingDate, BookingStatus.CONFIRMED, fee));

        if (opponentMemberId != null) {
            paymentObligationService.createPendingObligation(booking, opponentMemberId,
                    fee.divide(BigDecimal.TWO, 2, RoundingMode.HALF_UP));
        }
        return booking;
    }

    Optional<Booking> getBookingById(final UUID id) {
        return bookingRepository.findById(id);
    }

    @Transactional
    Booking cancelBooking(final UUID bookingId, final UUID memberId) {
        final var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingRejectedException(RejectionReason.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));
        if (!booking.getBookerMemberId().equals(memberId)) {
            throw new BookingRejectedException(RejectionReason.NOT_BOOKER,
                    "Only the booker can cancel booking " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingRejectedException(RejectionReason.ALREADY_CANCELLED,
                    "Booking is already cancelled: " + bookingId);
        }

        final var now = LocalDateTime.now(clock);
        final var slotStart = LocalDateTime.of(booking.getBookingDate(), booking.getSlotStart());
        if (!now.isBefore(slotStart)) {
            throw new BookingRejectedException(RejectionReason.BOOKING_ALREADY_STARTED,
                    "Booking has already started: " + bookingId);
        }

        final var fullRefund = !now.isAfter(slotStart.minus(properties.fullRefundNotice()));
        booking.setStatus(fullRefund ? BookingStatus.CANCELLED_FULL_REFUND : BookingStatus.CANCELLED_NO_REFUND);
        booking.setCancelledAt(now);
        final var cancelled = bookingRepository.save(booking);

        // With no refund the booker still pays for the court, so the opponent still owes their share
        if (fullRefund) {
            paymentObligationService.waivePendingObligations(cancelled);
        }
        return cancelled;
    }

    private void requireActiveMember(final UUID memberId, final LocalDate bookingDate) {
        final var member = memberServiceClient.getMember(memberId)
                .orElseThrow(() -> new BookingRejectedException(RejectionReason.MEMBER_NOT_FOUND,
                        "Member not found: " + memberId));
        if (bookingDate.isBefore(member.membershipStartDate()) || bookingDate.isAfter(member.membershipEndDate())) {
            throw new BookingRejectedException(RejectionReason.MEMBERSHIP_INACTIVE,
                    "Membership of " + memberId + " is not active on " + bookingDate);
        }
    }

    private void requireNoOverlappingBooking(final UUID memberId, final LocalDate bookingDate,
            final TimeSlotResponse slot) {
        if (bookingRepository.existsOverlappingBooking(memberId, bookingDate, slot.slotStart(), slot.slotEnd())) {
            throw new BookingRejectedException(RejectionReason.MEMBER_HAS_OVERLAPPING_BOOKING,
                    "Member " + memberId + " already has a booking overlapping " + slot.slotStart() + "-"
                            + slot.slotEnd() + " on " + bookingDate);
        }
    }

    // The checks above can race with a concurrent booking; the partial unique indexes are the safety net
    private Booking save(final Booking booking) {
        try {
            return bookingRepository.saveAndFlush(booking);
        } catch (final DataIntegrityViolationException e) {
            final var constraintName = e.getCause() instanceof final ConstraintViolationException cve
                    ? cve.getConstraintName()
                    : null;
            if (COURT_SLOT_INDEX.equals(constraintName)) {
                throw new BookingRejectedException(RejectionReason.SLOT_ALREADY_BOOKED,
                        "Court slot was booked concurrently", e);
            }
            if (MEMBER_SLOT_INDEX.equals(constraintName)) {
                throw new BookingRejectedException(RejectionReason.MEMBER_HAS_OVERLAPPING_BOOKING,
                        "Member booked the same slot concurrently", e);
            }
            throw e;
        }
    }
}
