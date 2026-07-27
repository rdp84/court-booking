package com.rdp.bookings.bookingservice.paymentobligation;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentObligationRepository extends JpaRepository<PaymentObligation, UUID> {
}
