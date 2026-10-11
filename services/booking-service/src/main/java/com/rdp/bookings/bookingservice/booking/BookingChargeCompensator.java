package com.rdp.bookings.bookingservice.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.rdp.bookings.bookingservice.client.MemberServiceClient;

// The charge is made in the Member Service, outside the booking's transaction, so a rollback here doesn't undo it.
// The refund is a no-op if the charge never landed (e.g. insufficient balance), and safe to retry by hand.
@Component
class BookingChargeCompensator {
    private static final Logger LOG = LoggerFactory.getLogger(BookingChargeCompensator.class);

    private final MemberServiceClient memberServiceClient;

    BookingChargeCompensator(final MemberServiceClient memberServiceClient) {
        this.memberServiceClient = memberServiceClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    void refundChargeForRolledBackBooking(final BookingChargeAttempted event) {
        try {
            memberServiceClient.refundBooking(event.memberId(), event.bookingId());
        } catch (final RuntimeException e) {
            LOG.error("Failed to refund member {} for rolled-back booking {}; refund it manually", event.memberId(),
                    event.bookingId(), e);
        }
    }
}
