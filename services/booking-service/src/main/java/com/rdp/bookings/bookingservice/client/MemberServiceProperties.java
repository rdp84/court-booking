package com.rdp.bookings.bookingservice.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services.member-service")
record MemberServiceProperties(String url) {
}
