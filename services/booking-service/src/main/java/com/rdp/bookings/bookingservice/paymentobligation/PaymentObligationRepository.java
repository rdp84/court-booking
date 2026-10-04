package com.rdp.bookings.bookingservice.paymentobligation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdp.bookings.bookingservice.booking.Booking;

interface PaymentObligationRepository extends JpaRepository<PaymentObligation, UUID> {

    List<PaymentObligation> findByBookingAndStatus(Booking booking, PaymentObligationStatus status);
}
