package com.rdp.bookings.bookingservice.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID courtId;
    private UUID timeSlotId;
    private UUID bookerMemberId;
    private UUID opponentMemberId;
    private LocalDate bookingDate;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private BigDecimal courtFee;
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    Booking(final UUID courtId, final UUID timeSlotId, final UUID bookerMemberId, final UUID opponentMemberId,
            final LocalDate bookingDate, final BookingStatus status, final BigDecimal courtFee) {
        this.courtId = courtId;
        this.timeSlotId = timeSlotId;
        this.bookerMemberId = bookerMemberId;
        this.opponentMemberId = opponentMemberId; // can be null, which represents a guest opponent
        this.bookingDate = bookingDate;
        this.status = status;
        this.courtFee = courtFee;
    }

    // Required by JPA
    Booking() {
    }

    UUID getId() {
        return id;
    }

    UUID getCourtId() {
        return courtId;
    }

    UUID getTimeSlotId() {
        return timeSlotId;
    }

    UUID getBookerMemberId() {
        return bookerMemberId;
    }

    UUID getOpponentMemberId() {
        return opponentMemberId;
    }

    LocalDate getBookingDate() {
        return bookingDate;
    }

    BookingStatus getStatus() {
        return status;
    }

    BigDecimal getCourtFee() {
        return courtFee;
    }

    LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    void setCancelledAt(final LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    void setStatus(final BookingStatus status) {
        this.status = status;
    }
}
