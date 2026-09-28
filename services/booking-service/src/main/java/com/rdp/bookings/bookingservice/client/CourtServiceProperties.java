package com.rdp.bookings.bookingservice.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "services.court-service")
@Validated
record CourtServiceProperties(@NotBlank String url) {
}
