package com.rdp.members.memberservice;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClockConfiguration {

    // Injected rather than calling now() directly so date-based rules (e.g. membership start date) are testable.
    // Uses the club's time zone, not the server's, since membership dates are in club local time.
    @Bean
    Clock clock(final ClubProperties clubProperties) {
        return Clock.system(clubProperties.timeZone());
    }
}
