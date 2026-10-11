package com.rdp.bookings.bookingservice.booking;

import java.util.UUID;

// Published just before the booker is charged, so the charge can be undone if the booking's transaction rolls back
record BookingChargeAttempted(UUID bookingId, UUID memberId) {
}
