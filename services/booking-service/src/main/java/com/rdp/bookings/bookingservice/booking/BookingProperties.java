package com.rdp.bookings.bookingservice.booking;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

// fullRefundNotice: cancelling at least this long before the slot starts gets a full refund
@ConfigurationProperties(prefix = "bookings.cancellation")
@Validated
record BookingProperties(@NotNull Duration fullRefundNotice) {
}
