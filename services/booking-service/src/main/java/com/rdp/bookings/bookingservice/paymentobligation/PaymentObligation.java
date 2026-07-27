package com.rdp.bookings.bookingservice.paymentobligation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.rdp.bookings.bookingservice.booking.Booking;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_obligations")
class PaymentObligation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    private UUID memberId;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentObligationStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    PaymentObligation(final Booking booking, final UUID memberId, final BigDecimal amount,
            final PaymentObligationStatus status) {
        this.booking = booking;
        this.memberId = memberId;
        this.amount = amount;
        this.status = status;
    }

    // Required by JPA
    PaymentObligation() {
    }

    UUID getId() {
        return id;
    }

    Booking getBooking() {
        return booking;
    }

    UUID getMemberId() {
        return memberId;
    }

    BigDecimal getAmount() {
        return amount;
    }

    PaymentObligationStatus getStatus() {
        return status;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
