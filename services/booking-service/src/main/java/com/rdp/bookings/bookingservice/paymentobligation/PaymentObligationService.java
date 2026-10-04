package com.rdp.bookings.bookingservice.paymentobligation;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.rdp.bookings.bookingservice.booking.Booking;

@Service
public class PaymentObligationService {
    private final PaymentObligationRepository paymentObligationRepository;

    PaymentObligationService(final PaymentObligationRepository paymentObligationRepository) {
        this.paymentObligationRepository = paymentObligationRepository;
    }

    public void createPendingObligation(final Booking booking, final UUID memberId, final BigDecimal amount) {
        paymentObligationRepository
                .save(new PaymentObligation(booking, memberId, amount, PaymentObligationStatus.PENDING));
    }

    public void waivePendingObligations(final Booking booking) {
        final var pending = paymentObligationRepository.findByBookingAndStatus(booking,
                PaymentObligationStatus.PENDING);
        pending.forEach(obligation -> obligation.setStatus(PaymentObligationStatus.WAIVED));
        paymentObligationRepository.saveAll(pending);
    }
}
