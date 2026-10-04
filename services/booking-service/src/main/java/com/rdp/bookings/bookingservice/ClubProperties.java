package com.rdp.bookings.bookingservice;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

// timeZone: the club's local time zone, which slot times and booking dates are expressed in
@ConfigurationProperties(prefix = "club")
@Validated
record ClubProperties(@NotNull ZoneId timeZone) {
}
