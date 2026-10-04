package com.rdp.bookings.bookingservice.paymentobligation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rdp.bookings.bookingservice.booking.Booking;

@ExtendWith(MockitoExtension.class)
class PaymentObligationServiceTest {

    @Mock
    PaymentObligationRepository paymentObligationRepository;

    @InjectMocks
    PaymentObligationService paymentObligationService;

    @Test
    void shouldSavePendingObligation() {
        final var booking = mock(Booking.class);
        final var memberId = UUID.randomUUID();

        paymentObligationService.createPendingObligation(booking, memberId, new BigDecimal("3.00"));

        final var captor = ArgumentCaptor.forClass(PaymentObligation.class);
        verify(paymentObligationRepository).save(captor.capture());
        final var saved = captor.getValue();
        assertThat(saved.getBooking()).isSameAs(booking);
        assertThat(saved.getMemberId()).isEqualTo(memberId);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(saved.getStatus()).isEqualTo(PaymentObligationStatus.PENDING);
    }
}
