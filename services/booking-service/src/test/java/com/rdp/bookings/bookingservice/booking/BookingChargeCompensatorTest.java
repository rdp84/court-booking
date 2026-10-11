package com.rdp.bookings.bookingservice.booking;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import com.rdp.bookings.bookingservice.client.MemberServiceClient;

class BookingChargeCompensatorTest {
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    private final MemberServiceClient memberServiceClient = mock(MemberServiceClient.class);
    private final BookingChargeCompensator compensator = new BookingChargeCompensator(memberServiceClient);

    @Test
    void shouldRefundTheBookingCharge() {
        compensator.refundChargeForRolledBackBooking(new BookingChargeAttempted(BOOKING_ID, MEMBER_ID));

        verify(memberServiceClient).refundBooking(MEMBER_ID, BOOKING_ID);
    }

    // The booking's transaction has already rolled back, so there's no request left to fail
    @Test
    void shouldSwallowRefundFailure() {
        willThrow(new ResourceAccessException("Connection refused")).given(memberServiceClient)
                .refundBooking(MEMBER_ID, BOOKING_ID);

        assertThatCode(
                () -> compensator.refundChargeForRolledBackBooking(new BookingChargeAttempted(BOOKING_ID, MEMBER_ID)))
                .doesNotThrowAnyException();
    }
}
