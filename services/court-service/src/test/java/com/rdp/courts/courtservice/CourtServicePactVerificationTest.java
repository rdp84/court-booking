package com.rdp.courts.courtservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;

@Provider("court-service")
@PactFolder("../pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourtServicePactVerificationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int port;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(final PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void setUp(final PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        jdbcTemplate.execute("DELETE FROM time_slots");
        jdbcTemplate.execute("DELETE FROM courts");
        jdbcTemplate.execute("DELETE FROM court_pricing");
    }

    @State("a court exists with id a3f8c2d1-0000-0000-0000-000000000001")
    void courtExists() {
        jdbcTemplate.execute("""
                INSERT INTO courts (id, name, is_active, created_at)
                VALUES ('a3f8c2d1-0000-0000-0000-000000000001', 'Court 1', true, NOW())
                """);
    }

    @State("no court exists with id a3f8c2d1-0000-0000-0000-000000000002")
    void courtDoesNotExist() {

    }

    @State("a price exists for the given date and time")
    void priceExists() {
        jdbcTemplate.execute("""
                INSERT INTO court_pricing (day_type, period_start, period_end, fee, valid_from)
                VALUES
                                ('WEEKDAY', '06:45', '17:00', 3.00, '2000-01-01'),
                                ('WEEKDAY', '17:00', '21:00', 6.00, '2000-01-01'),
                                ('WEEKDAY', '21:00', '22:45', 3.00, '2000-01-01'),
                                ('WEEKEND', '06:45', '22:45', 3.00, '2000-01-01')
                                            """);
    }

    @State("no price exists for the given date and time")
    void priceDoesNotExist() {

    }

    @State("a time slot exists with id a793bbf9-0000-0000-0000-000000000001")
    void timeSlotExists() {
        jdbcTemplate.execute("""
                    INSERT INTO courts (id, name, is_active, created_at)
                    VALUES ('a3f8c2d1-0000-0000-0000-000000000001', 'Court 1', true, NOW())
                    ON CONFLICT (id) DO NOTHING
                """);
        jdbcTemplate.execute(
                """
                            INSERT INTO time_slots (id, court_id, slot_start, created_at)
                            VALUES ('a793bbf9-0000-0000-0000-000000000001', 'a3f8c2d1-0000-0000-0000-000000000001', '06:45:00', NOW())
                        """);

    }

    @State("no time slot exists with id a793bbf9-0000-0000-0000-000000000002")
    void timeSlotDoesNotExist() {

    }
}
