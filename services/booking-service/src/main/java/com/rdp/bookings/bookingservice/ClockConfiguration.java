package com.rdp.bookings.bookingservice;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfiguration {

    // Injected rather than calling now() directly so time-based rules (e.g. the cancellation cutoff) are testable.
    // Uses the club's time zone, not the server's, since slot times and booking dates are in club local time.
    @Bean
    Clock clock(final ClubProperties clubProperties) {
        return Clock.system(clubProperties.timeZone());
    }
}
