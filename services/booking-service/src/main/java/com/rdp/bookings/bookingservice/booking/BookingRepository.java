package com.rdp.bookings.bookingservice.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface BookingRepository extends JpaRepository<Booking, UUID> {

    boolean existsByCourtIdAndTimeSlotIdAndBookingDateAndStatus(UUID courtId, UUID timeSlotId, LocalDate bookingDate,
            BookingStatus status);

    // Transaction-scoped: released automatically when the surrounding transaction commits or rolls back
    @Query(value = "SELECT 1 FROM pg_advisory_xact_lock(:key)", nativeQuery = true)
    int lockMember(@Param("key") long key);

    // Strict comparisons so back-to-back slots (one ending as the other starts) don't count as overlapping
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.bookingDate = :bookingDate
              AND b.status = com.rdp.bookings.bookingservice.booking.BookingStatus.CONFIRMED
              AND (b.bookerMemberId = :memberId OR b.opponentMemberId = :memberId)
              AND b.slotStart < :slotEnd
              AND b.slotEnd > :slotStart
            """)
    boolean existsOverlappingBooking(@Param("memberId") UUID memberId, @Param("bookingDate") LocalDate bookingDate,
            @Param("slotStart") LocalTime slotStart, @Param("slotEnd") LocalTime slotEnd);
}
