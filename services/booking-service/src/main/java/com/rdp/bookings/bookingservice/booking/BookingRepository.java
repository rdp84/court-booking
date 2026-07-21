package com.rdp.bookings.bookingservice.booking;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface BookingRepository extends JpaRepository<Booking, UUID> {
}
