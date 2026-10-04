package com.rdp.bookings.bookingservice;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
class BookingServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

    // Injected rather than calling now() directly so time-based rules (e.g. the cancellation cutoff) are testable
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
