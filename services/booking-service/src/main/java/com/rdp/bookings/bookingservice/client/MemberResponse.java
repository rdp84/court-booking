package com.rdp.bookings.bookingservice.client;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

record MemberResponse(UUID id, BigDecimal accountBalance, LocalDate membershipStartDate, LocalDate membershipEndDate) {
}
