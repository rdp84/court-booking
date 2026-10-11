package com.rdp.bookings.bookingservice.client;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface MemberServiceClient {
    Optional<MemberResponse> getMember(UUID memberId);

    // Idempotent on bookingId, so safe to retry
    ChargeResult chargeBooking(UUID memberId, UUID bookingId, BigDecimal amount);

    // Refunds whatever was charged for bookingId, at most once; a no-op if nothing was charged
    void refundBooking(UUID memberId, UUID bookingId);
}
