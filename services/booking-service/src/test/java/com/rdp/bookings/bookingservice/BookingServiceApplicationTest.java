package com.rdp.bookings.bookingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// Loads the whole application, so a bean that only exists through auto-configuration (e.g. RestClient.Builder)
// can't go missing unnoticed; the slice and unit tests build those by hand. The test application.yml replaces the
// main one, so the required properties are set here. Nothing is called at startup, so the URLs needn't be live.
@SpringBootTest(properties = { "services.member-service.url=http://localhost:8082",
        "services.court-service.url=http://localhost:8081", "club.time-zone=Europe/London",
        "bookings.cancellation.full-refund-notice=24h" })
@Testcontainers
class BookingServiceApplicationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Test
    void shouldStartApplicationContext() {
    }
}
