package com.rdp.bookings.bookingservice.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "services.member-service")
@Validated
record MemberServiceProperties(@NotBlank String url) {
}
